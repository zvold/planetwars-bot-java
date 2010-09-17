package bot.algorithms;

import java.util.Collection;
import java.util.List;

import shared.Planet;
import shared.Race;
import bot.SimulatorBot;
import continuous.Knapsack;
import filters.PlanetFilter;

public class FastExpand extends SimulatorBot.Algorithm {

    private static final float  FAST_EXPAND_FACTOR  = 1.25f;
    
    public FastExpand(SimulatorBot bot) {
        bot.super(bot);
    }
    
    @Override
    public boolean execute() {
        final Planet enemyHome = getFirstPlanet(Race.ENEMY);
        final Planet allyHome = getFirstPlanet(Race.ALLY);
        if (enemyHome == null || allyHome == null)
            return false; // fault tolerance
        
        // determine safe number of ships to depart from home,
        // assuming we're attacked on a next turn with max enemy ships
        // it's 105, but just in case...
        int enemyShips = bot()._sim.simulate(enemyHome, 1).ships();
        
        int dist = enemyHome.distance(allyHome);
        int allyShips = bot()._sim.simulate(allyHome, dist).ships();
        
        int shipsAvail = Math.min(allyHome.ships(), allyShips - enemyShips);
        log("# " + shipsAvail + " ships available for fast expand (" + dist + " distance)");
        
        // select all neutral planets closer to us than to the enemy
        List<Planet> neutrals = new PlanetFilter(bot()._game.planets(Race.NEUTRAL)) {
            @Override
            public boolean filter(Planet planet) {
                return FAST_EXPAND_FACTOR * planet.distance(allyHome) < planet.distance(enemyHome);
            }
        }.select(); 
        
        Collection<Planet> attack = Knapsack.solve(allyHome, neutrals, shipsAvail);
        
        if (attack.isEmpty())
            return false;
        
        log("# expanding to " + attack);
        int totalSent = 0;
        for (Planet target : attack) {
            assert(target.owner() == Race.NEUTRAL) : "check neutrality";
            int numShips = target.ships() + 1;
            issueOrder(allyHome, target, numShips);
            allyHome.addShips(-numShips);
            totalSent += numShips;
        }
        assert(totalSent <= shipsAvail) : "can't send more than available ships";
        
        log("# fastExpand() finished at " + bot()._timer.total() + " ms total");
        return true;
    }

    private Planet getFirstPlanet(Race owner) {
        Planet allyHome = null;
        for (Planet planet : bot()._game.planets(owner)) {
            allyHome = planet;
            break;
        }
        return allyHome;
    }
    
}
