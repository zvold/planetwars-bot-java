package bot;

import static shared.Race.ALLY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import shared.Planet;
import shared.Race;
import shared.Utils;
import simulate.MinShipsListener;
import simulate.OwnerChangeListener;
import simulate.Simulator;

import compare.CumulativeCloseness;
import compare.CumulativeDanger;
import compare.Pair;

import continuous.Adjacency;

public class AttackBot extends BaseBot implements OwnerChangeListener {

    private static final int TURNS_AHEAD = 3;
    private static final int TURNS_PREDICT = 30;
    private static final int EXTRA_SHIPS = 5;
    private static final int ALLY_RADIUS = 30;
    
    Adjacency _adj;
    Simulator _sim;
    MinShipsListener _minListener;
    boolean _ownerChanged;
    
    public static void main(String[] args) {
        parseArgs(args);
        AttackBot bot = new AttackBot();
        bot.run();
    }
    
    AttackBot() {
        _sim = new Simulator();
        _minListener = new MinShipsListener();
        _sim.addListener(_minListener);
        _sim.addListener(this);
    }
    
    @Override
    void doTurn() {
        if (_adj == null) {
            _adj = new Adjacency(_game.planets(), _timer);
        }
        // spend up to half the time for adjacency calculation
        _adj.doWork(Utils.timeout() / 2);
        Utils.log("# doTurn() started at " + _timer.totalTime() + " ms total");

        Comparator<Pair<Planet, Integer>> closeness = 
            new CumulativeCloseness<Planet, Integer>(_game.planets(Race.ALLY), TURNS_AHEAD);
        // map potential planets for attack to best turn number to attack
        ArrayList<Pair<Planet, Integer>> targets = new ArrayList<Pair<Planet, Integer>> ();
        ArrayList<Pair<Planet, Integer>> postponed = new ArrayList<Pair<Planet, Integer>> ();
    
        addPotentialTargets(targets, _game.planets(Race.NEUTRAL));
        addPotentialTargets(targets, _game.planets(Race.ENEMY));
        Collections.sort(targets, closeness);
        
        Utils.log("# " + targets.size() + " targets selected at " + _timer.totalTime() + " ms total");
        
        // map resource planets available to the safe disposable number of ships
        ArrayList <Pair<Planet, Integer>> sources = new ArrayList<Pair<Planet, Integer> >();
        for (Planet planet : _game.planets(Race.ALLY)) {
            // reset listeners
            this.reset(); 
            _minListener.reset();
            _sim.simulate(planet, TURNS_PREDICT);
            
            // skip not safe sources
            if (this._ownerChanged)
                continue;

            sources.add(new Pair<Planet, Integer>(planet, _minListener.ships(Race.ALLY)));
        }
        Collections.sort(sources, new CumulativeDanger<Planet, Integer>(_game.planets(Race.ENEMY), TURNS_AHEAD));
        
        Utils.log("# " + sources.size() + " sources selected at " + _timer.totalTime() + " ms total");
        
        while (_timer.totalTime() < (Utils.timeout() - 50) && !targets.isEmpty()) {
            Pair<Planet, Integer> tgtPair = targets.get(0);
            Planet target = tgtPair._key;
            int arrivalTurn = tgtPair._val + 1;
            
            // re-calculate ships needed
            Planet future = _sim.simulate(target, arrivalTurn);
            int shipsNeeded = future.ships() + EXTRA_SHIPS;
            
            Set<Pair<Planet, Integer>> toRemove = new HashSet<Pair<Planet,Integer>>();
            // select from sources as early as we can
            for (Pair<Planet, Integer> pair : sources) {
                Planet source = pair._key;
                if (source.distance(target) == arrivalTurn) { // if we arrive exactly when needed 
                    int shipsToSend = (pair._val >= shipsNeeded) ? 
                                      shipsNeeded : pair._val;
                    issueOrder(source, target, shipsToSend);
                    shipsNeeded -= shipsToSend;
                    source.setShips(source.ships() - shipsToSend);
                    pair._val = pair._val - shipsToSend;
                    if (source.ships() == 0)
                        toRemove.add(pair);
                }
                if (shipsNeeded <= 0)
                    break;
            }
            for (Pair<Planet, Integer> rem: toRemove)
                sources.remove(rem);

            if (shipsNeeded > 0) {
                Utils.log("# " + shipsNeeded + " ships postponed");
                postponed.add(tgtPair);
            }

            targets.remove(tgtPair);
            Utils.log("# " + (Utils.timeout() - _timer.totalTime()) + " ms remaining");
        }

        if (!postponed.isEmpty()) {
            Utils.log("# fall back to simple expansion strategy");
            Collections.sort(postponed, closeness);
        }
        
        while (_timer.totalTime() < (Utils.timeout() - 50) && !postponed.isEmpty()) {
            Pair<Planet, Integer> tgtPair = postponed.get(0);
            Planet target = tgtPair._key;

            // get set of several nearest allied planets
            Set<Planet> allies = getNearestAllies(target, _adj.neighbors(target));

            if (!allies.isEmpty()) {
                int totalShips = 0;
                for (Planet src : allies) {
                    Pair<Planet, Integer> ships = getSpareShips(sources, src);
                    totalShips += (ships != null) ? ships._val : (src.ships() / 2);
                }
                    
                int furthestSource = 0;
                for (Planet src : allies)
                    if (src.distance(target) > furthestSource)
                        furthestSource = src.distance(target);
    
                // re-calculate ships needed
                Planet future = _sim.simulate(target, furthestSource);
                int shipsNeeded = future.ships() + EXTRA_SHIPS;
            
                if (totalShips != 0 && totalShips >= shipsNeeded) {
                    int shipsSent = 0;
                    for (Planet src : allies) {
                        Pair<Planet, Integer> ships = getSpareShips(sources, src);
                        int shipsAvail = (ships != null) ? ships._val : (src.ships() / 2);
                        int num = shipsNeeded * shipsAvail / totalShips;
                        num = num > src.ships() ? src.ships() : num;
                        shipsSent += num;
                        issueOrder(src, target, num);
                        src.setShips(src.ships() - num);
                         
                        if (ships != null) {
                            ships._val = ships._val - shipsAvail;
                            if (ships._val <= 0)
                                sources.remove(ships);
                        }
                    }
                    Utils.log("# " + shipsSent + " ships sent");
                }
                
            }
            postponed.remove(tgtPair);
        }
        
        Utils.log("# " + (Utils.timeout() - _timer.totalTime()) + " ms remaining");
    }

    private Pair<Planet, Integer> getSpareShips(ArrayList<Pair<Planet, Integer>> sources, Planet src) {
        for (Pair<Planet, Integer> pair : sources)
            if (pair._key.equals(src))
                return pair;
        return null;
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
                if (Math.abs(planet.distance(target) - distance) < ALLY_RADIUS)
                    ret.add(planet);
                else
                    break;
            }
        }

        return ret;
    }
    
    private void addPotentialTargets(ArrayList<Pair<Planet, Integer>> targets, Set<Planet> planets) {
        for (Planet planet : planets) {
            _minListener.reset();
            Planet future = _sim.simulate(planet, TURNS_PREDICT);
            // skip if predicted to be allied
            if (future.owner() == Race.ALLY)
                continue;
            
            int minShips = Integer.MAX_VALUE;
            int minTurn = -1;
            for (Race owner : new Race[] {Race.NEUTRAL, Race.ENEMY})
                if (_minListener.ships(owner) < minShips) {
                    minShips = _minListener.ships(owner);
                    minTurn = _minListener.turn(owner);
                }
            
            // record this planet as a potential target
            assert(minTurn != -1) : "must have a enemy/neutral minimum";
            targets.add(new Pair<Planet, Integer>(planet, minTurn));
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
