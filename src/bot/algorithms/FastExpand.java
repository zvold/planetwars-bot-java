package bot.algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import shared.Planet;
import shared.Race;
import bot.SimulatorBot;
import continuous.Knapsack;

public class FastExpand extends SimulatorBot.Algorithm {

    private static final float  FAST_EXPAND_FACTOR  = 1.5f;
    
    public FastExpand(SimulatorBot bot) {
        bot.super(bot);
    }
    
    @Override
    public boolean execute() {
        Planet enemyHome = null;
        Planet allyHome = null;
        for (Planet planet : bot()._game.planets(Race.ALLY)) {
            allyHome = planet;
            break;
        }
        for (Planet planet : bot()._game.planets(Race.ENEMY)) {
            enemyHome = planet;
            break;
        }
        if (enemyHome == null || allyHome == null)
            return false; // fault tolerance
        
        // determine safe number of ships to depart from home,
        // assuming we're attacked on a next turn with max enemy ships
        Planet future = bot()._sim.simulate(enemyHome, 1);
        int enemyShips = future.ships(); // it's 105, but just in case...
        
        int dist = enemyHome.distance(allyHome);
        future = bot()._sim.simulate(allyHome, dist);
        int allyShips = future.ships();
        
        int shipsAvail = Math.min(allyHome.ships(), allyShips - enemyShips);
        log("# " + shipsAvail + " ships available for expand (" + dist + " distance)");
        
        // select all neutral planets closer to us than to the enemy
        List<Planet> neutrals = new ArrayList<Planet>();
        for (Planet planet : bot()._game.planets(Race.NEUTRAL))
            if (FAST_EXPAND_FACTOR * planet.distance(allyHome) < planet.distance(enemyHome))
                neutrals.add(planet);
        
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
        
        log("# fastExpand() finished at " + bot()._timer.totalTime() + " ms total");
        return true;
    }

}
