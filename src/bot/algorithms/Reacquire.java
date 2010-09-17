package bot.algorithms;

import static bot.SimulatorBot.TURNS_PREDICT;

import java.util.ArrayList;
import java.util.Collections;

import shared.Planet;
import shared.Race;
import shared.Utils;
import bot.SimulatorBot;

import compare.IScore;

public class Reacquire extends SimulatorBot.Algorithm {

    IScore<Planet> _closeness;
    int               _extraShips;
    
    public Reacquire(SimulatorBot bot, IScore<Planet> closeness, int extraShips) {
        bot.super(bot);
        _closeness = closeness;
        _extraShips = extraShips;
    }
    
    @Override
    public boolean execute() {
        ArrayList<Planet> losing = new ArrayList<Planet>();
        for (Planet planet : bot()._game.planets()) {
            Planet future = bot()._sim.simulate(planet, TURNS_PREDICT);
            if (bot()._lastLostListener.lost() && 
                future.owner() == Race.ENEMY &&
                bot()._minShipsListener.wasOwned(Race.ALLY)) {
                log("# reacquiring " + planet);
                log("# " + bot()._lastLostListener.ships() + " ships predicted in " 
                         + bot()._lastLostListener.turn() + " turns");
                losing.add(planet);
            }
        }
        Collections.sort(losing, _closeness);
        
        while (bot()._timer.totalTime() < (Utils.timeout() - 50) && !losing.isEmpty()) {
            // get best potential planet to attack
            Planet target = losing.remove(0);

            bot()._sim.simulate(target, TURNS_PREDICT);
            int turnLost = bot()._lastLostListener.turn();
            
            new WaitingAttack(bot(), target, _extraShips, turnLost, false).execute();
        }
        log("# reacquire() finished at " + bot()._timer.totalTime() + " ms total");
        return true;
    }
    
}
