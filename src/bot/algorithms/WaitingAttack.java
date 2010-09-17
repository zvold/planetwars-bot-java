package bot.algorithms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import shared.Planet;
import shared.Race;
import simulate.ShipsGraphListener;
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
    
    private Map<Planet, ShipsGraphListener> getAvailableShips(List<Planet> sources) {
        Map<Planet, ShipsGraphListener> ret = new HashMap<Planet, ShipsGraphListener>(sources.size());
        for (Planet src : sources) {
            ShipsGraphListener graph = new ShipsGraphListener(Race.ALLY);
            bot()._sim.addListener(graph);
            bot()._sim.simulate(src, EXPAND_WAIT + SimulatorBot.TURNS_PREDICT);
            bot()._sim.clearEndTurnListeners();
            ret.put(src, graph);
        }
        return ret;
    }
    
    /**
     * @param target
     * @param sources
     * @param avail
     * @return - Turn number when arriving fleets from specified sources are
     *         able to overrun enemy on the planet.
     */
    private int isEnoughShips(Planet target, List<Planet> sources, Map<Planet, ShipsGraphListener> avail) {
        int furthestSource = bot().getFurthest(target, sources);

        for (int i=0; i<EXPAND_WAIT; i++) {
            Planet future = bot()._sim.simulate(target, furthestSource + i);
            if (future.owner() == Race.ALLY)
                continue;
            int shipsNeeded = future.ships() + _extraShips;
            
            int shipsAvail = 0;
            for (Planet src : sources) {
                int ships = avail.get(src).shipsAvail(i + furthestSource - src.distance(target));
                ships /= 2;
                shipsAvail += ships;
            }
            if (shipsAvail >= shipsNeeded)
                return i;
        }
        return Integer.MAX_VALUE;
    }
    
    @Override
    public boolean execute() {
        // get a set of several nearest allied planets
        List<Planet> sources = bot().getNearestInRadius(_target, bot()._adj.neighbors(_target), 
                                                        Race.ALLY, ATTACK_RADIUS);
        //Collections.sort(sources, new SimpleCloseness(bot()._game.planets(Race.ENEMY)));
        //Collections.reverse(sources);
        
        if (!sources.isEmpty()) {
            log("# WaitingAttack on " + _target + ", " + sources.size() + " sources");
            
            Map<Planet, ShipsGraphListener> avail = getAvailableShips(sources);
            int minTurn = Integer.MAX_VALUE;
            int minSrcs = 0;
            for (int i=1; i<=sources.size(); i++) {
                int enoughTurn = isEnoughShips(_target, sources.subList(0, i), avail);
                if (enoughTurn < minTurn) {
                    minTurn = enoughTurn;
                    minSrcs = i;
                }
            }
            if (minTurn != Integer.MAX_VALUE) {
                log("#\t enough ships found, waiting " + minTurn + ", from " + minSrcs + " sources");
                new CoordinatedAttack(bot(), _target, sources.subList(0, minSrcs),
                                      minTurn, avail, _extraShips).execute();
            }
        }
        return true;
    }
    
}
