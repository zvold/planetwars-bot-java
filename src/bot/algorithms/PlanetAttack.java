package bot.algorithms;

import java.util.List;

import shared.Planet;
import shared.Race;
import bot.SimulatorBot;

public class PlanetAttack extends SimulatorBot.Algorithm {

    private static final int    ATTACK_RADIUS       = 10;
    
    Planet      _target;
    boolean     _safe;
    int         _extraShips;
    
    public PlanetAttack(SimulatorBot bot, Planet target, int extraShips, boolean safe) {
        bot.super(bot);
        _target = target;
        _safe = safe;
        _extraShips = extraShips;
    }
    
    @Override
    public boolean execute() {
        // get a set of several nearest allied planets
        List<Planet> sources = bot().getNearestInRadius(_target, bot()._adj.neighbors(_target), 
                                                        Race.ALLY, ATTACK_RADIUS);
        if (sources.isEmpty())
            return false;
        
        bot()._game.clearAllData();
        int totalShips = 0;
        int furthestSource = 0;
        int shipsAvail;

        for (Planet src : sources) {
             shipsAvail = bot().shipsAvailableSafe(src, Race.ALLY);
            if (shipsAvail == 0)
                continue;
            if (src.distance(_target) > furthestSource)
                furthestSource = src.distance(_target);
            src.setData(shipsAvail);
            totalShips += shipsAvail;
        }

        Planet future = bot()._sim.simulate(_target, furthestSource + 1);
        int shipsNeeded = future.ships() + _extraShips;

        if (totalShips >= shipsNeeded || !_safe) {
            int shipsSent = 0;
            int fromSources = 0;
            double error = 0.0;
            for (Planet src : sources) {
                if (src.data() == null)
                    continue;
                shipsAvail = (Integer)src.data();
                double frac = (double)shipsNeeded * (double)shipsAvail / (double)totalShips;
                int num = (int)Math.floor(frac);
                error += frac - (double)num;
                if (error > 0.9999) {
                    error -= 0.9999;
                    num++;
                }
                assert(num <= src.ships()) : "calc correctness " + num + " <> " + src.ships();
                if (num == 0) // don't bother
                    continue;
                shipsSent += num;
                fromSources++;
                issueOrder(src, _target, num);
                src.addShips(-num);
            }
            assert(shipsSent == shipsNeeded) : "correct number of ships: " + 
                                               shipsSent + " <> " + shipsNeeded + ", " + error;

            log("# " + _target + " attacked with " + shipsSent
                     + " ships from " + fromSources + " sources");
            log("# future " + future + " was predicted");
        } else 
            return false;
        return true;
    }

}
