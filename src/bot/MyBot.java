package bot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import shared.FutureOrder;
import shared.Planet;
import shared.Race;
import shared.Utils;
import simulate.FirstLoseRecorder;
import simulate.LastLoseRecorder;
import simulate.MinShipsListener;
import simulate.OwnerChangeListener;
import simulate.Simulator;

import compare.CumulativeCloseness;
import compare.IScore;
import compare.PlanetsAdjComparator;
import compare.SimpleCloseness;

import continuous.Adjacency;
import continuous.Dijkstra;

public class MyBot extends BaseBot implements OwnerChangeListener {

    public static final int TURNS_EXTRA = 5;
    public static final int TURNS_PREDICT = 50;
    public static final int EXTRA_SHIPS = 1;
    public static final int POSTPONE_LIMIT = 10;

    // threshold for decision making if we don't attack planets from attack list tail
    public static final double THRESH_POSTPONE_ATTACK = 0.75d;
    // attack list tail definition
    public static final double THRESH_ATTACK_TAIL = 0.5d;
    
    Adjacency           _adj;
    Simulator           _sim;
    MinShipsListener    _minListener;
    boolean             _ownerChanged;
    IScore<Planet>      _cumulComp;
    IScore<Planet>      _enemyComp;
    IScore<Planet>      _allyComp;
    int                 _turnsPosponed;
    Set<Planet>         _postponed = new HashSet<Planet>();
    LastLoseRecorder    _lastLostListener;
    FirstLoseRecorder   _allyToEnemyLostListener;
    
    public static void main(String[] args) {
        parseArgs(args);
        MyBot bot = new MyBot();
        bot.run();
    }

    MyBot() {
        _sim = new Simulator();
        _minListener = new MinShipsListener();
        _sim.addListener(_minListener);
        _sim.addListener(this);
        
        // listener for *->enemy ownership changes
        _lastLostListener = new LastLoseRecorder(Race.ENEMY);
        _sim.addListener(_lastLostListener);

        // listener for ally->enemy ownership changes
        _allyToEnemyLostListener= new FirstLoseRecorder(Race.ALLY, Race.ENEMY);
        _sim.addListener(_allyToEnemyLostListener);
    }
    
    @Override
    public void doTurn() {
        // spend up to half the time for adjacency calculation
        if (_adj == null) {
            _adj = new Adjacency(_game.planets(), _timer, this);
        }
        _adj.doWork(Utils.timeout() / 2);
        log("# doTurn() started at " + _timer.totalTime() + " ms total");

        _allyComp = new PlanetsAdjComparator(_game.planets(Race.ALLY));
        _cumulComp = new CumulativeCloseness(_game.planets());
        _enemyComp = new SimpleCloseness(_game.planets(Race.ENEMY));
        
        sneakDefence();
        sneakAttack();
        
        defence();
        attack();
        reinforce();
        
        log("# doTurn() finished at " + _timer.totalTime() + " ms total");
    }

    private void attack() {
        ArrayList<Planet> attack = new ArrayList<Planet>();
        selectForAttack(_game.planets(Race.NEUTRAL), attack);
        selectForAttack(_game.planets(Race.ENEMY), attack);
        Collections.sort(attack, _cumulComp);

        log("# " + attack.size() + " targets selected at " + _timer.totalTime() + " ms total");

        int attackNum = 0;
        int attackTotal = attack.size();
        boolean forceSend = _game.planets(Race.ALLY).size() >=
                            ((double)_game.planets().size() * THRESH_POSTPONE_ATTACK);
        boolean postpone = false;
        _postponed.clear();
        while (_timer.totalTime() < (Utils.timeout() - 50) && !attack.isEmpty()) {
            // get best potential planet to attack
            Planet target = attack.get(0);

            if (_turnsPosponed > POSTPONE_LIMIT)
                forceSend = true;
            
            // postpone all orders if we are operating on the attack list tail
            if (!forceSend) {
                if (attackNum++ >= (double)attackTotal * THRESH_ATTACK_TAIL)
                    postpone = true;
            }

            _turnsPosponed = postpone ? _turnsPosponed++ : 0;
            
            // get set of several nearest allied planets
            Set<Planet> sources = getNearestAllies(target, _adj.neighbors(target));

            if (!sources.isEmpty()) {
                int totalShips = 0;
                for (Planet src : sources) {
                    _sim.simulate(src, src.distance(target) + 1);
                    if (_ownerChanged)
                        continue;
                    totalShips += _minListener.ships(Race.ALLY);
                }

                int furthestSource = 0;
                for (Planet src : sources)
                    if (src.distance(target) > furthestSource)
                        furthestSource = src.distance(target);

                // TODO: future may be allied, so predict neede ships number more accurately
                Planet future = _sim.simulate(target, furthestSource + 1);
                int shipsNeeded = future.ships() + EXTRA_SHIPS;

                if (totalShips != 0 && totalShips >= shipsNeeded) {
                    int shipsSent = 0;
                    for (Planet src : sources) {
                        _minListener.reset();
                        this.reset();
                        _sim.simulate(src, src.distance(target) + 1);
                        if (_ownerChanged)
                            continue;
                        int shipsAvail = _minListener.ships(Race.ALLY);
                        int num = (int)(0.5d + (double)shipsNeeded *
                                               (double)shipsAvail / (double)totalShips);
                        num = num > src.ships() ? src.ships() : num;
                        shipsSent += num;
                        if (!postpone)
                            issueOrder(src, target, num);
                        else
                            _postponed.add(src);
                        // HACK: note how we still substract to prevent ships 
                        // from being used as reinforcements
                        src.setShips(src.ships() - num);
                    }
                    log("# " + target + " attacked with "
                                   + shipsSent + " ships from " 
                                   + sources.size() + " sources"
                                   + (postpone ? " [postponed]" : ""));
                    log("# future " + future + " was predicted");
                }
            }
            attack.remove(target);
        }
    }

    private void defence() {
        Simulator simul = new Simulator();
        FirstLoseRecorder loseRecorder = new FirstLoseRecorder(Race.ENEMY);
        simul.addListener(loseRecorder);

        ArrayList<Planet> defence = new ArrayList<Planet>();
        for (Planet planet : _game.planets(Race.ALLY)) {
            Planet future = simul.simulate(planet, TURNS_PREDICT);
            if (future.owner() == Race.ALLY)
                continue;
            if (loseRecorder.lost()) {
                log("# " + planet + " is predicted to be lost at " + 
                                 loseRecorder.turn() + " turn to " +
                                 loseRecorder.ships() + " ships");
                defence.add(planet);
            }
        }
        Collections.sort(defence, _cumulComp);
       
        log("# " + defence.size() + " defendants selected at " + _timer.totalTime() + " ms total");
        
        while (_timer.totalTime() < (Utils.timeout() - 50) && !defence.isEmpty()) {
            // get best potential planet to attack
            Planet target = defence.remove(0);

            // simulate to get required ships and turn
            loseRecorder.reset();
            Planet future = simul.simulate(target, TURNS_PREDICT);
            
            assert(future.owner() != Race.ALLY) : "check future race correctness";
            assert(loseRecorder.lost()) : "check defence list correctness";
            
            int turnBefore = loseRecorder.turn();
            int shipsNeeded = loseRecorder.ships();
            
            // go through all allied neighbors within turnBefore radius and see if they can help
            int totalShips = 0;
            for (Planet src : _adj.neighbors(target)) {
                if (src.distance(target) > turnBefore)
                    break;
                _minListener.reset();
                this.reset();
                _sim.simulate(src, TURNS_PREDICT);
                if (_ownerChanged)
                    continue;
                totalShips += _minListener.ships(Race.ALLY);
            }
            
            // if we can help, send proportional amounts
            if (totalShips >= shipsNeeded) {
                int shipsSent = 0;
                for (Planet src : _adj.neighbors(target)) {
                    if (src.distance(target) > turnBefore)
                        break;
                    _minListener.reset();
                    this.reset();
                    _sim.simulate(src, TURNS_PREDICT);
                    if (_ownerChanged)
                        continue;
                    int shipsAvail = _minListener.ships(Race.ALLY);
                    int num = (int)(0.5d + (double)shipsNeeded *
                                           (double)shipsAvail / (double)totalShips);
                    num = num > src.ships() ? src.ships() : num;
                    shipsSent += num;
                    issueOrder(src, target, num);
                    src.setShips(src.ships() - num);
                }
                log("# " + target + " receives "
                               + shipsSent + " rescue ships in "
                               + turnBefore + " turns");
            }
        }
        
    }

    private void selectForAttack(Set<Planet> planets, ArrayList<Planet> attack) {
        for (Planet planet : planets) {
            Planet future = _sim.simulate(planet, TURNS_PREDICT);
            if (future.owner() == Race.ALLY)
                continue;

            attack.add(planet);
        }
    }

    private Set<Planet> getNearestAllies(Planet target, ArrayList<Planet> neighbors) {
        Set<Planet> ret = new HashSet<Planet>();
        ArrayList<Planet> allies = new ArrayList<Planet>();
        for (Planet planet : neighbors)
            if (planet.owner() == Race.ALLY)
                allies.add(planet);

        if (!allies.isEmpty()) {
            Planet source = allies.get(0);
            ret.add(source);
            int distance = source.distance(target);
            for (int i = 1; i < allies.size(); i++) {
                Planet planet = allies.get(i);
                
                assert (planet.distance(target) >= distance) : 
                        "check sorting" + planet.distance(target) + " >= " + distance;
                
                if (Math.abs(planet.distance(target) - distance) < TURNS_EXTRA)
                    ret.add(planet);
                else
                    break;
            }
        }

        return ret;
    }

    // 
    // OwnerChangeListener interface methods
    //
    
    @Override
    public void ownerChanged(int turn, Race fromRace, int fromShips, Race toRace, int toShips) {
        _ownerChanged = true;
    }

    @Override
    public void reset() {
        _ownerChanged = false;
    }

    @Override
    public int turn() {
        // TODO Auto-generated method stub
        return 0;
    }

    private void sneakAttack() {
        // select planets having neutral->enemy transition and remaining with enemy
        ArrayList<Planet> attack = new ArrayList<Planet>();
        for (Planet planet : _game.planets()) {
            Planet future = _sim.simulate(planet, TURNS_PREDICT);
            if (future.owner() == Race.ENEMY && _lastLostListener.lost())
                attack.add(planet);
        }
        Collections.sort(attack, _allyComp);
        Collections.reverse(attack);

        sneakAttack(attack);
    }

    private void sneakDefence() {
        // select planets having ally->enemy transition and remaining with enemy
        ArrayList<Planet> attack = new ArrayList<Planet>();
        for (Planet planet : _game.planets()) {
            Planet future = _sim.simulate(planet, TURNS_PREDICT);
            if (future.owner() == Race.ENEMY && _allyToEnemyLostListener.lost())
                attack.add(planet);
        }
        Collections.sort(attack, _allyComp);
        Collections.reverse(attack);
        
        sneakAttack(attack);
    }

    private void sneakAttack(ArrayList<Planet> attack) {
        log("# " + attack.size() + " targets selected at " + _timer.totalTime() + " ms total");
        
        while (_timer.totalTime() < (Utils.timeout() - 50) && !attack.isEmpty()) {
            // get best potential planet to attack
            Planet target = attack.remove(0);

            // simulate to determine best turn to attack
            int shipsNeeded = 0;
            int attackTurn = 0;
            Planet future = _sim.simulate(target, TURNS_PREDICT);
            if (future.owner() == Race.ENEMY) {
                if (_allyToEnemyLostListener.lost()) {
                    attackTurn = _allyToEnemyLostListener.turn();
                    future = _sim.simulate(target, attackTurn);
                    shipsNeeded = future.ships();
                } else if (_lastLostListener.lost()) {
                    attackTurn = _lastLostListener.turn() + 1;
                    future = _sim.simulate(target, attackTurn);
                    shipsNeeded = future.ships() + EXTRA_SHIPS;
                } else
                    assert(false) : "sneakAttack() was called for invalid planet";
            }

            ArrayList<Planet> sources = new ArrayList<Planet>();
            selectCloserThan(sources, _game.planets(Race.ALLY), target, attackTurn);
            //selectAllToSum(sources, target, shipsNeeded * 2, attackTurn);
            
            int totalShips = 0;
            int maxAvail = 0;        // for "1 ship" correction
            Planet maxPlanet = null; // for "1 ship" correction
            for (Planet src : sources) {
                _sim.simulate(src, attackTurn); // TODO: can be attackTurn - src.distance(target) ?
                if (_ownerChanged)
                    continue;
                int avail = _minListener.ships(Race.ALLY);
                totalShips += avail;
                if (avail > maxAvail) {
                    maxAvail = avail;
                    maxPlanet = src;
                }
            }
            
            if (totalShips == 0)
                continue;
            assert(maxPlanet != null) : "we have sources";
            
            if (totalShips >= shipsNeeded) {
                int shipsSent = 0;
                int fromSources = 0;
                for (Planet src : sources) {
                    _sim.simulate(src, attackTurn); // TODO: can be attackTurn - src.distance(target) ?
                    if (_ownerChanged)
                        continue;
                    int avail = _minListener.ships(Race.ALLY);
                    int num = (int)(0.5d + (double)shipsNeeded *
                                           (double)avail / (double)totalShips);
                    num = num > src.ships() ? src.ships() : num;
                    
                    int allyETA = src.distance(target);          // allied ETA
                    if (allyETA == attackTurn) {
                        issueOrder(src, target, num);
                        src.addShips(-num);
                    } else if (allyETA < attackTurn) {
                        FutureOrder order = new FutureOrder(src, target, num, attackTurn - allyETA);
                        log("# future order " + order + " created");
                        _game.addFutureOrder(order);
                    } else
                        assert(false) : "source selected is too far away"; 
                    shipsSent += num;
                    fromSources++;
                }
                
                if (shipsSent < shipsNeeded) {
                    int need = shipsNeeded - shipsSent;
                    if (maxPlanet.ships() >= need) {
                        int allyETA = maxPlanet.distance(target);          // allied ETA
                        if (allyETA == attackTurn) {
                            issueOrder(maxPlanet, target, need);
                            maxPlanet.addShips(-need);
                            log("# " + need + " correction ships sent");                            
                        } else if (allyETA < attackTurn) {
                            FutureOrder order = new FutureOrder(maxPlanet, target, need, attackTurn - allyETA);
                            log("# future order " + order + " created (correction)");
                            _game.addFutureOrder(order);
                        } else
                            assert(false) : "source selected is too far away"; 
                    }
                }
                
                log("# " + target + " attacked with " + shipsSent + " ships from " 
                         + fromSources + " sources");
                log("# " + (shipsNeeded - (target.owner() == Race.ALLY ? 0 : EXTRA_SHIPS))
                         + " ships on turn " + attackTurn + " was predicted");
            }
        }
    }

    private void selectCloserThan(ArrayList<Planet> select,
            Set<Planet> planets, Planet target, int distance) {
        for (Planet planet : planets)
            if (planet != target && planet.distance(target) <= distance)
                select.add(planet);
    }

    private void reinforce() {
        if (_game.planets(Race.ENEMY).isEmpty())
            return;

        // compose list of potential targets to reinforce
        ArrayList<Planet> targets = new ArrayList<Planet>();
        for (Planet planet : _game.planets(Race.ALLY)) {
            Planet future = _sim.simulate(planet, TURNS_PREDICT);
            if (future.owner() == Race.ALLY)
                targets.add(planet);
        }
        // sorted by closeness to enemy
        Collections.sort(targets, _enemyComp);
        Collections.reverse(targets);
        
        Dijkstra dijkstra = new Dijkstra(_timer, this);
        Set<Planet> visited = new HashSet<Planet>(_game.planets(Race.ALLY).size());
        int totalSent = 0;
        while (_timer.totalTime() < Utils.timeout() - 50 && !targets.isEmpty()) {
            Planet target = targets.remove(0);
            // add to visited so ships are not transferred from the end point
            visited.add(target);
            dijkstra.calculate(target, _game.planets(Race.ALLY), null);
            double targetScore = _enemyComp.score(target);
            
            for (Planet src : _game.planets(Race.ALLY)) {
                // skip planets already sent reinforcements
                if (visited.contains(src))
                    continue;

                // don't reinforce if src is in more dangerous position than target
                Planet enemy = _adj.getNearestNeighbor(src, Race.ENEMY);
                assert(enemy != null) : "should've returned if there is no enemies";
                if (src.distance(enemy) < src.distance(target))
                    continue;
                
                double sourceScore = _enemyComp.score(src);
                
                // if the target is further from the enemy than the source, don't reinforce
                if (targetScore < sourceScore)
                    continue;
                
                _sim.simulate(src, TURNS_PREDICT);
                if (_minListener.ships(Race.ALLY) == 0 || _ownerChanged)
                    continue;
                
                int shipsAvail = _minListener.ships(Race.ALLY);

                if (dijkstra.backEdge(src) != null) {
                    issueOrder(src, dijkstra.backEdge(src), shipsAvail);
                    totalSent += shipsAvail;
                    visited.add(src);
                }
                
                // HACK: better not modify knowing that reinforcement() call is the last one
                // this decreases number of ships flying back and forth meaninglessly
                //src.setShips(src.ships() - shipsAvail);
            }
        }
        
        if (totalSent != 0)
            log("# " + totalSent + " ships sent as reinforcements");
    }
    
}
