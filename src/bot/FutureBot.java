package bot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import shared.FutureOrder;
import shared.Planet;
import shared.Race;
import shared.Utils;
import simulate.LastLoseRecorder;
import simulate.MinShipsListener;
import simulate.OwnerChangeListener;
import simulate.Simulator;

import compare.IScore;
import compare.PlanetsAdjComparator;

import continuous.Adjacency;

public class FutureBot extends BaseBot implements OwnerChangeListener {

    public static final int TURNS_PREDICT = 50;
    public static final int EXTRA_SHIPS = 1;

    Adjacency           _adj;
    IScore<Planet>      _allyComp;
    Simulator           _sim;
    LastLoseRecorder   _lostListener;
    LastLoseRecorder   _acqListener;
    MinShipsListener    _minListener;
    boolean             _ownerChanged;
    
    public static void main(String[] args) {
        parseArgs(args);
        FutureBot bot = new FutureBot();
        bot.run();
    }
    
    private FutureBot() {
        _sim = new Simulator();
        _lostListener = new LastLoseRecorder(Race.ENEMY);
        _sim.addListener(_lostListener);
        _acqListener = new LastLoseRecorder(Race.ALLY);
        _sim.addListener(_acqListener);
        _minListener = new MinShipsListener();
        _sim.addListener(_minListener);
        _sim.addListener(this);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void doTurn() {
        // spend up to half the time for adjacency calculation
        if (_adj == null) {
            _adj = new Adjacency(_game.planets(), _timer, this);
        }
        _adj.doWork(Utils.timeout() / 2);
        log("# doTurn() started at " + _timer.totalTime() + " ms total");

        _allyComp = new PlanetsAdjComparator(_game.planets(Race.ALLY));
        
        _game.advanceFutureOrders();
        carryOutFutureOrders();
        
        sneakAttack(_game.planets(Race.ALLY));
        sneakAttack(_game.planets(Race.NEUTRAL), _game.planets(Race.ENEMY));
        log("# sneakAttack() finished at " + _timer.totalTime() + " ms total");
    }

    private void sneakAttack(Collection<Planet>... colls) {
        ArrayList<Planet> attack = new ArrayList<Planet>();
        for (Collection<Planet> planets : colls)
            selectForAttack(planets, attack);
        Collections.sort(attack, _allyComp);
        Collections.reverse(attack);

        log("# " + attack.size() + " targets selected at " + _timer.totalTime() + " ms total");
        
        while (_timer.totalTime() < (Utils.timeout() - 50) && !attack.isEmpty()) {
            // get best potential planet to attack
            Planet target = attack.remove(0);

            // simulate to determine best turn to attack
            int shipsNeeded = 0;
            int attackTurn = 0;
            Planet future = _sim.simulate(target, TURNS_PREDICT);
            if (_ownerChanged) {
                attackTurn = _lostListener.turn() + (target.owner() == Race.ALLY ? 0 : 1);
                if (_acqListener.lost() && _acqListener.turn() == attackTurn)
                    continue;
                future = _sim.simulate(target, attackTurn);
                shipsNeeded = future.ships() + (target.owner() == Race.ALLY ? 0 : EXTRA_SHIPS);
            } else
                continue;

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
                    int avail = _minListener.ships(Race.ALLY) != Integer.MAX_VALUE ?
                                _minListener.ships(Race.ALLY) : 0;
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

//    private void selectAllToSum(ArrayList<Planet> sources, Planet target, 
//                                int shipsNeeded, int attackTurn) {
//        int sumShips = 0;
//        for (Planet planet : _adj.neighbors(target)) {
//            if (target.distance(planet) > attackTurn)
//                return;
//            if (planet.owner() == Race.ALLY) {
//                _sim.simulate(planet, TURNS_PREDICT);
//                if (_ownerChanged)
//                    continue;
//                sumShips += _minListener.ships(Race.ALLY) != Integer.MAX_VALUE ?
//                            _minListener.ships(Race.ALLY) : 0;
//                sources.add(planet);
//            }
//            if (sumShips >= shipsNeeded)
//                return;
//        }
//    }

    private void selectCloserThan(ArrayList<Planet> select, Set<Planet> planets, 
                                  Planet target, int distance) {
        for (Planet planet : planets)
            if (planet != target && planet.distance(target) <= distance)
                select.add(planet);
    }

    private void selectForAttack(Collection<Planet> planets, ArrayList<Planet> attack) {
        for (Planet planet : planets) {
            Planet future = _sim.simulate(planet, TURNS_PREDICT);
            if (future.owner() != Race.ALLY)
                attack.add(planet);
        }
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


