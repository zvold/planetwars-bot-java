package bot.algorithms;

import static bot.SimulatorBot.TURNS_PREDICT;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import shared.Planet;
import shared.Race;
import shared.Utils;
import bot.SimulatorBot;

import compare.IScore;

import continuous.Dijkstra;
import filters.PlanetFilter;

public class Reinforce extends SimulatorBot.Algorithm {

    public static final float DANGER_THRESH = 1.1f;
    
    IScore<Planet> _closeness;
    
    public Reinforce(SimulatorBot bot, IScore<Planet> closeness) {
        bot.super(bot);
        _closeness = closeness;
    }
    
    @Override
    public boolean execute() {
        log("# Reinforce() started");
        
        if (bot()._game.planets(Race.ENEMY).isEmpty())
            return false;

        // compose list of potential targets to reinforce
        List<Planet> targets = new PlanetFilter(bot()._game.planets()) {
            @Override
            public boolean filter(Planet planet) {
                return bot()._sim.simulate(planet, TURNS_PREDICT).owner() == Race.ALLY;
            }
        }.select();
        // sorted by closeness to enemy
        Collections.sort(targets, _closeness);
        
        Dijkstra dijkstra = new Dijkstra(bot()._timer, bot());
        Set<Planet> visited = new HashSet<Planet>(targets.size());
        int totalSent = 0;
        while (bot()._timer.total() < Utils.timeout() - 50 && !targets.isEmpty()) {
            Planet target = targets.remove(0);
            // add to visited so ships are not transferred from the end point
            visited.add(target);
            dijkstra.calculate(target, bot()._game.planets(Race.ALLY));
            
            for (Planet src : bot()._game.planets(Race.ALLY)) {
                // skip planets already sent reinforcements
                if (visited.contains(src))
                    continue;

                // don't reinforce if src is in more dangerous position than target
                Planet enemy = bot()._adj.getNearestNeighbor(src, Race.ENEMY);
                assert(enemy != null) : "should've returned if there is no enemies";
                if (DANGER_THRESH * src.distance(enemy) < src.distance(target) &&
                    src.ships() < enemy.ships())
                    continue;
                
                int shipsAvail = bot().shipsAvailable(src, Race.ALLY);
                if (shipsAvail == 0)
                    continue;
                
                if (dijkstra.backEdge(src) != null) {
                    issueOrder(src, dijkstra.backEdge(src), shipsAvail);
                    totalSent += shipsAvail;
                    visited.add(src);
                }
                
                src.addShips(-shipsAvail);
            }
        }
        
        if (totalSent != 0)
            log("#\t " + totalSent + " ships sent as reinforcements");
        log("#\t reinforce() finished at " + bot()._timer.total() + " ms total");
        
        return true;
    }
    
    
}
