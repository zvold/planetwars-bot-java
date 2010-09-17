package bot.algorithms;

import static bot.SimulatorBot.TURNS_PREDICT;

import java.util.Collections;
import java.util.List;

import shared.Planet;
import shared.Race;
import shared.Utils;
import bot.SimulatorBot;

import compare.IScore;

import filters.PlanetFilter;

public class Reacquire extends SimulatorBot.Algorithm {

    IScore<Planet>    _closeness;
    int               _extraShips;
    
    public Reacquire(SimulatorBot bot, IScore<Planet> closeness, int extraShips) {
        bot.super(bot);
        _closeness = closeness;
        _extraShips = extraShips;
    }
    
    @Override
    public boolean execute() {
        log("# Reacquire() started");
        List<Planet> losing = new PlanetFilter(bot()._game.planets()) {
            @Override
            public boolean filter(Planet planet) {
                Planet future = bot()._sim.simulate(planet, TURNS_PREDICT);
                if (bot()._lastLostListener.changed() && 
                    future.owner() == Race.ENEMY &&
                    bot()._minShipsListener.wasOwned(Race.ALLY)) {
                    log("#\t reacquiring " + planet);
                    log("#\t " + bot()._lastLostListener.ships() + " ships predicted in " 
                               + bot()._lastLostListener.turn() + " turns");
                    return true;            
                }
                return false;
            }
        }.select();
        Collections.sort(losing, _closeness);
        
        while (bot()._timer.total() < (Utils.timeout() - 50) && !losing.isEmpty()) {
            // get best potential planet to attack
            Planet target = losing.remove(0);

            bot()._sim.simulate(target, TURNS_PREDICT);
            int turnLost = bot()._lastLostListener.turn();
            
            new WaitingAttack(bot(), target, _extraShips, turnLost, false).execute();
        }
        log("#\t reacquire() finished at " + bot()._timer.total() + " ms total");
        return true;
    }
    
}
