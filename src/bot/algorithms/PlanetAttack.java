package bot.algorithms;

import java.util.ArrayList;

import shared.Planet;
import shared.Race;
import bot.SimulatorBot;

public class PlanetAttack extends SimulatorBot.Algorithm {

    private static final int    ATTACK_RADIUS       = 5;
    
    Planet     _target;
    boolean _safe;
    int        _extraShips;
    
    public PlanetAttack(SimulatorBot bot, Planet target, int extraShips, boolean safe) {
        bot.super(bot);
        _target = target;
        _safe = safe;
        _extraShips = extraShips;
    }
    
    @Override
    public boolean execute() {
        // get a set of several nearest allied planets
        ArrayList<Planet> sources = bot().getNearestInRadius(_target, bot()._adj.neighbors(_target), 
                                                             Race.ALLY, ATTACK_RADIUS);

        if (!sources.isEmpty()) {
            int totalShips = 0;
            int maxAvail = 0;
            Planet maxPlanet = null;
            int furthestSource = 0;

            int shipsAvail;
            for (Planet src : sources) {
                if ((shipsAvail = bot().shipsAvailable(src, Race.ALLY)) == 0)
                    continue;
                if (src.distance(_target) > furthestSource)
                    furthestSource = src.distance(_target);
                totalShips += shipsAvail;
                if (shipsAvail > maxAvail) {
                    maxAvail = shipsAvail;
                    maxPlanet = src;
                }
            }

            Planet future = bot()._sim.simulate(_target, furthestSource + 1);
            int shipsNeeded = future.ships() + _extraShips;

            if (totalShips != 0 && (totalShips >= shipsNeeded || !_safe)) {
                int shipsSent = 0;
                int fromSources = 0;
                for (Planet src : sources) {
                    if ((shipsAvail = bot().shipsAvailable(src, Race.ALLY)) == 0)
                        continue;
                    int num = (int) (0.5d + (double) shipsNeeded
                            * (double) shipsAvail / (double) totalShips);
                    assert(num <= src.ships()) : "calc correctness";
                    shipsSent += num;
                    fromSources++;
                    issueOrder(src, _target, num);
                    src.addShips(-num);
                }

                if (shipsSent < shipsNeeded) {
                    int need = shipsNeeded - shipsSent;
                    if (maxPlanet != null && maxPlanet.ships() >= need) {
                        issueOrder(maxPlanet, _target, need);
                        shipsSent += need;
                        maxPlanet.addShips(-need);
                        log("# " + need + " correction ships sent");
                    }
                }

                log("# " + _target + " attacked with " + shipsSent
                         + " ships from " + fromSources + " sources");
                log("# future " + future + " was predicted");
            }
        }
        return true;
    }

}
