package bot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import shared.Fleet;
import shared.Planet;
import shared.Race;
import shared.Utils;
import simulate.LastLoseRecorder;
import simulate.MinShipsListener;
import simulate.OwnerChangeListener;
import simulate.Simulator;

import compare.CumulativeCloseness;
import compare.IScore;
import compare.SimpleCloseness;
import compare.SimpleGrowthCloseness;

import continuous.Adjacency;
import continuous.Dijkstra;

public class SimpleBot extends BaseBot implements OwnerChangeListener {

    public static final int TURNS_EXTRA = 5;
    public static final int TURNS_PREDICT = 50;
    public static final int EXTRA_SHIPS = 1;
    public static final int POSTPONE_LIMIT = 10;

    // threshold for decision making if we don't attack planets from attack list tail
    public static final double THRESH_POSTPONE_ATTACK = 0.75d;
    // attack list tail definition
    public static final double THRESH_ATTACK_TAIL = 0.5d;
    
    Random _rnd = new Random(System.currentTimeMillis());
    Adjacency _adj;
    Simulator _sim;
    MinShipsListener _minListener;
    boolean _ownerChanged;
    IScore<Planet> cumulComp;
    IScore<Planet> enemyComp;
    IScore<Planet> alliedComp;
    int _turnsPosponed;
    Set<Planet> _postponed = new HashSet<Planet>();
    
    public static void main(String[] args) {
        parseArgs(args);
        SimpleBot bot = new SimpleBot();
        bot.run();
    }

    SimpleBot() {
        _sim = new Simulator();
        _minListener = new MinShipsListener();
        _sim.addListener(_minListener);
        _sim.addListener(this);
    }
    
    @Override
    public void doTurn() {
        // spend up to half the time for adjacency calculation
        if (_adj == null) {
            _adj = new Adjacency(_game.planets(), _timer, this);
        }
        _adj.doWork(Utils.timeout() / 2);
        log("# doTurn() started at " + _timer.totalTime() + " ms total");

        cumulComp = new CumulativeCloseness(_game.planets());
        enemyComp = new SimpleCloseness(_game.planets(Race.ENEMY));
        alliedComp = new SimpleGrowthCloseness(_game.planets(Race.ALLY));
        
        defence();
        log("# defence() finished at " + _timer.totalTime() + " ms total");

        neutral();
        log("# neutral() finished at " + _timer.totalTime() + " ms total");
        
        attack();
        log("# attack() finished at " + _timer.totalTime() + " ms total");
        
        reinforce();
        log("# reinforce() finished at " + _timer.totalTime() + " ms total");
    }

    private void neutral() {
        if (_game.planets(Race.NEUTRAL).isEmpty())
            return;
        
        int enemyAttack = 0;
        int alliedAttack = 0;
        for (Planet planet : _game.planets(Race.NEUTRAL)) {
            if (planet.incoming(Race.ALLY).isEmpty() && !planet.incoming(Race.ENEMY).isEmpty()) {
                for (Fleet fleet : planet.incoming(Race.ENEMY))
                    enemyAttack += fleet.ships();
            }
            if (planet.incoming(Race.ENEMY).isEmpty() && !planet.incoming(Race.ALLY).isEmpty()) {
                for (Fleet fleet : planet.incoming(Race.ALLY))
                    alliedAttack += fleet.ships();
            }
        }
        
        int attackDiff = enemyAttack - alliedAttack;
        if (attackDiff <= 0)
            return;
        log("# keep up with enemy expand for " + attackDiff + " ships");
        
        // sort all neutral planets by closeness to self
        ArrayList<Planet> neutrals = new ArrayList<Planet>(_game.planets(Race.NEUTRAL));
        Collections.sort(neutrals, alliedComp);
        Collections.reverse(neutrals);

        while (attackDiff > 0 && !neutrals.isEmpty()) {
            Planet neutral = neutrals.remove(0);
            int shipsNeeded = neutral.ships() + EXTRA_SHIPS;
            
            boolean fill = false;
            for (Planet source : _adj.neighbors(neutral)) {
                if (source.owner() != Race.ALLY)
                    continue;
                Planet future = _sim.simulate(source, TURNS_PREDICT);
                if (future.owner() != Race.ALLY || _postponed.contains(source))
                    continue;
                
                if (_minListener.ships(Race.ALLY) == 0)
                    continue;
                int shipsAvail = _minListener.ships(Race.ALLY);
                
                int shipsToSend = shipsAvail > shipsNeeded ? shipsNeeded : shipsAvail;
                issueOrder(source, neutral, shipsToSend);
                source.setShips(source.ships() - shipsToSend);
                shipsNeeded -= shipsToSend;
                attackDiff -= shipsToSend;
                if (shipsNeeded <= 0 || attackDiff <=0) {
                    fill = true;
                    break;
                }
            }
            if (!fill)
                break;
        }
        if (attackDiff > 0)
            log("# " + attackDiff + " ships short for expansion");
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
        Collections.sort(targets, enemyComp);
        Collections.reverse(targets);
        
        Dijkstra dijkstra = new Dijkstra(_timer, this);
        Set<Planet> visited = new HashSet<Planet>(_game.planets(Race.ALLY).size());
        int totalSent = 0;
        while (_timer.totalTime() < Utils.timeout() - 50 && !targets.isEmpty()) {
            Planet target = targets.remove(0);
            // add to visited so ships are not transferred from the end point
            visited.add(target);
            dijkstra.calculate(target, _game.planets(Race.ALLY), null);
            double targetScore = enemyComp.score(target);
            
            for (Planet src : _game.planets(Race.ALLY)) {
                // planets with postponed orders don't reinforce
                // also skip planets already sent reinforcements
                if (_postponed.contains(src) || visited.contains(src))
                    continue;

                // don't reinforce if src is in more dangerous position than target
                Planet enemy = _adj.getNearestNeighbor(src, Race.ENEMY);
                assert(enemy != null) : "should've returned if there is no enemies";
                if (src.distance(enemy) < src.distance(target))
                    continue;
                
                double sourceScore = enemyComp.score(src);
                
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

    private void attack() {
        ArrayList<Planet> attack = new ArrayList<Planet>();
        selectForAttack(_game.planets(Race.NEUTRAL), attack);
        selectForAttack(_game.planets(Race.ENEMY), attack);
        Collections.sort(attack, cumulComp);

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
                int maxAvail = 0;
                Planet maxPlanet = null;
                int furthestSource = 0;

                // TODO: future may be allied, so predict neede ships number more accurately
                Simulator simul = new Simulator();
                LastLoseRecorder loseRecorder = new LastLoseRecorder(Race.ENEMY);
                simul.addListener(loseRecorder);
                Planet future = simul.simulate(target, TURNS_PREDICT);
                int minTurn = 0; // minimum turn we're allowed to be there
                if (loseRecorder.lost()) {
                    minTurn = loseRecorder.turn() + 1;
                }
                int shipsNeeded = future.ships() + EXTRA_SHIPS;
                
                for (Planet src : sources) {
                    if (target.owner() == Race.NEUTRAL && minTurn != 0 && 
                        src.distance(target) < minTurn)
                        continue;
                    _sim.simulate(src, src.distance(target) + 1);
                    if (_ownerChanged)
                        continue;
                    if (src.distance(target) > furthestSource)
                        furthestSource = src.distance(target);
                    int avail = _minListener.ships(Race.ALLY);
                    totalShips += avail;
                    if (avail > maxAvail) {
                        maxAvail = avail;
                        maxPlanet = src;
                    }
                }

                if (totalShips != 0 && totalShips >= shipsNeeded) {
                    int shipsSent = 0;
                    for (Planet src : sources) {
                        if (target.owner() == Race.NEUTRAL && minTurn != 0 && 
                            src.distance(target) < minTurn)
                            continue;
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
                    
                    if (shipsSent < shipsNeeded) {
                        int need = shipsNeeded - shipsSent;
                        if (maxPlanet != null && maxPlanet.ships() >= need) {
                            if (!postpone)
                                issueOrder(maxPlanet, target, need);
                            shipsSent += need;
                            maxPlanet.setShips(maxPlanet.ships() - need);
                            log("# " + need + " correction ships sent");
                        }
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
        LastLoseRecorder loseRecorder = new LastLoseRecorder(Race.ENEMY);
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
        Collections.sort(defence, cumulComp);
       
        log("# " + defence.size() + " defendants selected at " + _timer.totalTime() + " ms total");
        
        while (_timer.totalTime() < (Utils.timeout() - 50) && !defence.isEmpty()) {
            // get best potential planet to attack
            Planet target = defence.remove(0);

            // simulate to get required ships and turn
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

}
