package bot.algorithms;

import java.util.ArrayList;
import java.util.List;

import shared.FutureOrder;
import shared.Planet;
import shared.Race;
import bot.SimulatorBot;

public class WaitingAttack extends SimulatorBot.Algorithm {

    private static final int    ATTACK_RADIUS       = 5;
    private static final int    EXPAND_WAIT         = 5;
    
    Planet     _target;
    int        _turnLost;
    int        _extraShips;
    boolean    _safe;
    
    public WaitingAttack(SimulatorBot bot, Planet target, int extraShips, int turnLost, boolean safe) {
        bot.super(bot);
        _target = target;
        _turnLost = turnLost;
        _extraShips = extraShips;
        _safe = safe;
    }
    
    @Override
    public boolean execute() {
        // get a set of several nearest allied planets
        ArrayList<Planet> sources = bot().getNearestInRadius(_target, bot()._adj.neighbors(_target), 
                                                             Race.ALLY, ATTACK_RADIUS);

        if (!sources.isEmpty()) {
            AvailableShips avail = new AvailableShips(_target, sources, EXPAND_WAIT);
            int turn;
            int shipsNeeded = 0;
            Planet future = null;
            for (turn=0; turn<EXPAND_WAIT; turn++) {
                int flightTime = Math.max(_turnLost, turn) + avail._furthest[turn] + 1;
                future = bot()._sim.simulate(_target, flightTime);
                
                int enemyShips = 0;
                if (_safe) {
                    ArrayList<Planet> enemyNeigh = bot().getNearestInRadius(_target, 
                                                                            bot()._adj.neighbors(_target), 
                                                                            Race.ENEMY, ATTACK_RADIUS);
                    enemyShips = bot().sumFutureShipsTarget(_target, enemyNeigh, flightTime);
                }
                
                shipsNeeded = future.ships() + enemyShips + _extraShips;
                if (avail._shipsAvail[turn] >= shipsNeeded)
                    break;
            }
            if (turn >= EXPAND_WAIT)
                turn--;
            int totalShips = avail._shipsAvail[turn];
            
            if (totalShips != 0 && totalShips >= shipsNeeded) {
                log("# found enough ships around at turn " + turn);
                int shipsSent = 0;
                int fromSources = 0;
                int shipsAvail;
                for (Planet src : sources) {
                    shipsAvail = (turn == 0) ?
                                 bot().shipsAvailable(src, Race.ALLY) :
                                 bot().shipsAvailable(bot()._sim.simulate(src, turn), Race.ALLY);
                    if (shipsAvail == 0)
                        continue;
                    int num = (int) (0.5d + (double) shipsNeeded
                                          * (double) shipsAvail / (double) totalShips);
                    // can be in future
                    // assert(num <= src.ships()) : "calc correctness";
                    shipsSent += num;
                    fromSources++;
                    if (turn == 0) {
                        issueOrder(src, _target, num);
                        src.addShips(-num);
                    } else {
                        FutureOrder order = new FutureOrder(src, _target, num, turn + 1);
                        log("# future order " + order + " created");
                        bot()._game.addFutureOrder(order);
                    }
                }

                if (shipsSent < shipsNeeded) {
                    int need = shipsNeeded - shipsSent;
                    if (avail._maxPlanet[turn] != null && avail._maxPlanet[turn].ships() >= need
                        && need > 0) {
                        if (turn == 0) {
                            issueOrder(avail._maxPlanet[turn], _target, need);
                            avail._maxPlanet[turn].addShips(-need);
                            log("# " + need + " correction ships sent");
                        } else {
                            FutureOrder order = new FutureOrder(avail._maxPlanet[turn], _target, need, turn);
                            log("# future order " + order + " created (correction)");
                            bot()._game.addFutureOrder(order);
                        }
                        shipsSent += need;
                    }
                }
                log("# " + _target + " attacked with " + shipsSent
                         + " ships from " + fromSources + " sources");
                log("# future " + future + " was predicted");
            }
        }
        return true;
    }

    public class AvailableShips {

        List<Planet> _sources;
        List<Planet> _origSrc;
        int[]        _shipsAvail;
        int[]        _furthest;
        int[]        _maxAvail;
        Planet[]     _maxPlanet;
        Planet       _target;
        
        public AvailableShips(Planet target, List<Planet> sources, int turns) {
            int size = sources.size();
            _sources = new ArrayList<Planet>(size);
            _origSrc = new ArrayList<Planet>(sources);
            // guaranteed to be initialized by JLS
            _shipsAvail = new int[turns];
            _furthest   = new int[turns];
            _maxAvail   = new int[turns];
            _maxPlanet  = new Planet[turns];
            _target     = target;
            for (Planet source : sources)
                _sources.add(source.deepCopy());
            
            // TODO: implement this as 1 pass
            for (int i=0; i<turns; i++) {
                fillData(i);
                for (int j=0; j<size; j++)
                    _sources.set(j, bot()._sim.simulate(_sources.get(j), 1));
            }
        }
        
        void fillData(int turn) {
            int shipsAvail;
            for (int i=0; i<_sources.size(); i++) {
                Planet src = _sources.get(i);
                if ((shipsAvail = bot().shipsAvailable(src, Race.ALLY)) == 0)
                    continue;
                if (src.distance(_target) > _furthest[turn])
                    _furthest[turn] = src.distance(_target);
                _shipsAvail[turn] += shipsAvail;
                if (shipsAvail > _maxAvail[turn]) {
                    _maxAvail[turn] = shipsAvail;
                    _maxPlanet[turn] = _origSrc.get(i);
                }
            }
        }
        
    }
    
}
