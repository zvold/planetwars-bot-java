package bot.algorithms;

import static bot.SimulatorBot.TURNS_PREDICT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import shared.Planet;
import shared.Race;
import shared.Utils;
import bot.SimulatorBot;

import compare.IScore;

import continuous.Dijkstra;

public class Reinforce extends SimulatorBot.Algorithm {

    IScore<Planet> _closeness;
    
    public Reinforce(SimulatorBot bot, IScore<Planet> closeness) {
        bot.super(bot);
        _closeness = closeness;
    }
    
    @Override
    public boolean execute() {
        if (bot()._game.planets(Race.ENEMY).isEmpty())
            return false;

        // compose list of potential targets to reinforce
        ArrayList<Planet> targets = new ArrayList<Planet>();
        for (Planet planet : bot()._game.planets(Race.ALLY)) {
            Planet future = bot()._sim.simulate(planet, TURNS_PREDICT);
            if (future.owner() == Race.ALLY)
                targets.add(planet);
        }
        // sorted by closeness to enemy
        Collections.sort(targets, _closeness);
        
        Dijkstra dijkstra = new Dijkstra(bot()._timer, bot());
        Set<Planet> visited = new HashSet<Planet>(bot()._game.planets(Race.ALLY).size());
        int totalSent = 0;
        while (bot()._timer.totalTime() < Utils.timeout() - 50 && !targets.isEmpty()) {
            Planet target = targets.remove(0);
            // add to visited so ships are not transferred from the end point
            visited.add(target);
            dijkstra.calculate(target, bot()._game.planets(Race.ALLY), null);
            
            for (Planet src : bot()._game.planets(Race.ALLY)) {
                // skip planets already sent reinforcements
                if (visited.contains(src))
                    continue;

                // don't reinforce if src is in more dangerous position than target
                Planet enemy = bot()._adj.getNearestNeighbor(src, Race.ENEMY);
                assert(enemy != null) : "should've returned if there is no enemies";
                if (1.1f * src.distance(enemy) < src.distance(target) &&
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
            log("# " + totalSent + " ships sent as reinforcements");
        log("# reinforce() finished at " + bot()._timer.totalTime() + " ms total");
        
        return true;
    }
    
    
}
