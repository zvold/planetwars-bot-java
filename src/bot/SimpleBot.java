package bot;

import static shared.Race.ALLY;
import static shared.Race.ENEMY;
import static shared.Race.NEUTRAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import shared.Planet;
import shared.Race;
import shared.Utils;
import simulate.FirstLoseRecorder;
import simulate.MinShipsListener;
import simulate.OwnerChangeListener;
import simulate.Simulator;
import continuous.Adjacency;

public class SimpleBot extends BaseBot implements OwnerChangeListener {

    public static final int TURNS_EXTRA = 5;
    public static final int TURNS_PREDICT = 50;
    public static final int EXTRA_SHIPS = 1;
    
    Random _rnd = new Random(System.currentTimeMillis());
    Adjacency _adj;
    Simulator _sim;
    MinShipsListener _minListener;
    boolean _ownerChanged;
    CumulativeCloseness comp; 
    
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
    void doTurn() {
        // spend up to half the time for adjacency calculation
        if (_adj == null) {
            _adj = new Adjacency(_game.planets(), _timer);
        }
        _adj.doWork(Utils.timeout() / 2);
        Utils.log("# doTurn() started at " + _timer.totalTime() + " ms total");

        comp = new CumulativeCloseness(_game.planets());
        
        defence();
        attack();
        reinforce();
        
        Utils.log("# doTurn() finished at " + _timer.totalTime() + " ms total");
    }

    private void reinforce() {
        // don't spend time for fancy reinforcements when we're losing
        if (_game.growth(Race.ALLY) < _game.growth(ENEMY))
            return;
        for (Planet planet : _game.planets(Race.ALLY)) {
            _minListener.reset();
            this.reset();
            _sim.simulate(planet, TURNS_PREDICT);
            if (_minListener.ships(Race.ALLY) == Integer.MAX_VALUE || _ownerChanged)
                continue;
            int shipsAvail = _minListener.ships(Race.ALLY);
            
            for (Planet neigh : _adj.neighbors(planet)) {
                if (neigh.owner() != Race.ALLY)
                    continue;
                
                Planet enemy = _adj.getNearestNeighbor(planet, Race.ENEMY);
                if (enemy != null && enemy.distance(planet) < neigh.distance(planet))
                    continue;
                
                if (comp.score(neigh) < comp.score(planet)) {
                    Utils.log("# " + shipsAvail + " reinforcement went from " + planet);
                    issueOrder(planet, neigh, shipsAvail);
                    
                    // HACK: better not modify knowing that reinforcement() call is the last one
                    // this decreases number of ships flying back and forth meaninglessly
                    //planet.setShips(planet.ships() - shipsAvail);
                    break;
                }
            }
        }
    }

    private void attack() {
        ArrayList<Planet> attack = new ArrayList<Planet>();
        selectForAttack(_game.planets(NEUTRAL), attack);
        selectForAttack(_game.planets(ENEMY), attack);
        Collections.sort(attack, comp);

        Utils.log("# " + attack.size() + " targets selected at " + _timer.totalTime() + " ms total");

        while (_timer.totalTime() < (Utils.timeout() - 50) && !attack.isEmpty()) {
            // get best potential planet to attack
            Planet target = attack.get(0);

            // get set of several nearest allied planets
            Set<Planet> sources = getNearestAllies(target, _adj.neighbors(target));

            if (!sources.isEmpty()) {
                int totalShips = 0;
                for (Planet src : sources) {
                    _minListener.reset();
                    this.reset();
                    _sim.simulate(src, src.distance(target) + 1);
                    if (_ownerChanged)
                        continue;
                    totalShips += _minListener.ships(Race.ALLY) != Integer.MAX_VALUE ?
                                  _minListener.ships(Race.ALLY) : 0;
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
                        int shipsAvail = _minListener.ships(Race.ALLY) != Integer.MAX_VALUE ?
                                         _minListener.ships(Race.ALLY) : 0;
                        int num = (int)(0.5d + (double)shipsNeeded *
                                               (double)shipsAvail / (double)totalShips);
                        num = num > src.ships() ? src.ships() : num;
                        shipsSent += num;
                        issueOrder(src, target, num);
                        src.setShips(src.ships() - num);
                    }
                    Utils.log("# " + target + " attacked with "
                                   + shipsSent + " ships from " 
                                   + sources.size() + " sources");
                    Utils.log("# future " + future + " was predicted");
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
            loseRecorder.reset();
            Planet future = simul.simulate(planet, TURNS_PREDICT);
            if (future.owner() == Race.ALLY)
                continue;
            if (loseRecorder.lost()) {
                Utils.log("# " + planet + " is predicted to be lost at " + 
                                 loseRecorder.turn() + " turn to " +
                                 loseRecorder.ships() + " ships");
                defence.add(planet);
            }
        }
        Collections.sort(defence, comp);
       
        Utils.log("# " + defence.size() + " defendants selected at " + _timer.totalTime() + " ms total");
        
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
                totalShips += _minListener.ships(Race.ALLY) != Integer.MAX_VALUE ?
                              _minListener.ships(Race.ALLY) : 0;
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
                    int shipsAvail = _minListener.ships(Race.ALLY) != Integer.MAX_VALUE ?
                                     _minListener.ships(Race.ALLY) : 0;
                    int num = (int)(0.5d + (double)shipsNeeded *
                                           (double)shipsAvail / (double)totalShips);
                    num = num > src.ships() ? src.ships() : num;
                    shipsSent += num;
                    issueOrder(src, target, num);
                    src.setShips(src.ships() - num);
                }
                Utils.log("# " + target + " receives "
                               + shipsSent + " rescue ships in "
                               + turnBefore + " turns");
            }
        }
        
    }

    private void selectForAttack(Set<Planet> planets, ArrayList<Planet> attack) {
        for (Planet planet : planets) {
            this.reset();
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
            if (planet.owner() == ALLY)
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

    class CumulativeCloseness implements Comparator<Planet> {

        Collection<Planet> _planets;

        CumulativeCloseness(Collection<Planet> planets) {
            _planets = planets;
        }

        @Override
        public int compare(Planet p1, Planet p2) {
            return (int) (score(p1) - score(p2));
        }

        double score(Planet target) {
            double score = 0;
            for (Planet planet : _planets) {
                if (planet.owner() == Race.NEUTRAL || planet == target)
                    continue;
                int sign = (planet.owner() == Race.ALLY) ? -1 : 1;
                score += (double)sign * (double)planet.ships() / (double)planet.distance(target);
            }
            score *= (double)target.growth() / (double)target.ships();
            score *= 100.0d;
            
            return score;
        }

    }

    @Override
    public void ownerChanged(int turn, Race fromRace, int fromShips, Race toRace, int toShips) {
        _ownerChanged = true;
    }

    @Override
    public void reset() {
        _ownerChanged = false;
    }

}
