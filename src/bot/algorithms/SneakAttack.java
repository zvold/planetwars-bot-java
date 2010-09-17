package bot.algorithms;
import static bot.SimulatorBot.TURNS_PREDICT;

import java.util.Collections;
import java.util.List;

import shared.FutureOrder;
import shared.Planet;
import shared.Race;
import shared.Utils;
import bot.SimulatorBot;

import compare.IScore;

import filters.PlanetFilter;

public class SneakAttack extends SimulatorBot.Algorithm {

    private static final float  SNEAK_SHIPS_FACTOR  = 1.5f;
    
    IScore<Planet> _closeness;
    int               _extraShips;
    
    public SneakAttack(SimulatorBot bot, IScore<Planet> closeness, int extraShips) {
        bot.super(bot);
        _closeness = closeness;
        _extraShips = extraShips;
    }
    
    @Override
    public boolean execute() {
        log("# sneakAttack()/sneakDefence() started");
        
        // first, try to defend allied planets that we lose to the enemy
        sneakDefence();
        log("#\t sneakDefence() finished at " + bot()._timer.total() + " ms total");
        
        // second, try to sneakily intercept neutral planets transferred to enemy
        sneakAttack();
        log("#\t sneakAttack() finished at " + bot()._timer.total() + " ms total");
        
        return true;
    }

    private void sneakAttack() {
        // select planets having neutral->enemy transition and remaining with enemy
        List<Planet> attack = new PlanetFilter(bot()._game.planets()) {
            @Override
            public boolean filter(Planet planet) {
                Planet future = bot()._sim.simulate(planet, TURNS_PREDICT);
                return (future.owner() == Race.ENEMY && bot()._lastN2ELostListener.changed());
            }
        }.select(); 
        Collections.sort(attack, _closeness);

        sneakAttack(attack);
    }

    private void sneakDefence() {
        // select planets having ally->enemy transition and remaining with enemy
        List<Planet> attack = new PlanetFilter(bot()._game.planets()) {
            @Override
            public boolean filter(Planet planet) {
                Planet future = bot()._sim.simulate(planet, TURNS_PREDICT);
                return (future.owner() == Race.ENEMY && bot()._firstA2ELostListener.changed());
            }
        }.select(); 
        Collections.sort(attack, _closeness);
        sneakAttack(attack);
    }

    private void sneakAttack(List<Planet> attack) {
        if (attack.isEmpty())
            return;
        log("#\t " + attack.size() + " targets selected at " + bot()._timer.total() + " ms total");
        
        while (bot()._timer.total() < (Utils.timeout() - 50) && !attack.isEmpty()) {
            // get best potential planet to attack
            Planet target = attack.remove(0);

            // simulate to determine best turn to attack
            int shipsNeeded = 0;
            int attackTurn = 0;
            Planet future = bot()._sim.simulate(target, TURNS_PREDICT);
            if (future.owner() == Race.ENEMY) {
                if (bot()._firstA2ELostListener.changed()) {
                    attackTurn = bot()._firstA2ELostListener.turn();
                    future = bot()._sim.simulate(target, attackTurn);
                    shipsNeeded = future.ships();
                } else if (bot()._lastN2ELostListener.changed()) {
                    attackTurn = bot()._lastN2ELostListener.turn() + 1;
                    future = bot()._sim.simulate(target, attackTurn);
                    shipsNeeded = future.ships() + _extraShips;
                } else
                    assert(false) : "sneakAttack() was called for invalid planet";
            }

//            List<Planet> sources = bot().selectCloserThan(Race.ALLY, target, attackTurn);
            List<Planet> sources = bot().selectOwnerToSum(Race.ALLY, target, 
                                                          (int)(shipsNeeded * SNEAK_SHIPS_FACTOR), 
                                                          attackTurn);
            if (sources.isEmpty())
                continue;
            
            bot()._game.clearAllData();
            int totalShips = 0;
            int shipsAvail;
            for (Planet src : sources) {
                // TODO: simulate can be for (attackTurn - src.distance(target)) turns ?
                if ((shipsAvail = bot().shipsAvailable(src, Race.ALLY)) == 0)
                    continue;
                totalShips += shipsAvail;
                src.setData(shipsAvail);
            }
            
            if (totalShips == 0)
                continue;
            
            if (totalShips >= shipsNeeded) {
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
                    assert(num <= src.ships()) : "calc correctness";
                    if (num == 0) // don't bother
                        continue;
                    int allyETA = src.distance(target);          // allied ETA
                    if (allyETA == attackTurn) {
                        issueOrder(src, target, num);
                        src.addShips(-num);
                    } else if (allyETA < attackTurn) {
                        FutureOrder order = new FutureOrder(Race.ALLY, src, target, num, attackTurn - allyETA);
                        log("# future order " + order + " created");
                        bot()._game.addFutureOrder(order);
                    } else
                        assert(false) : "source selected is too far away"; 
                    shipsSent += num;
                    fromSources++;
                }
                assert(shipsSent == shipsNeeded) : "correct number of ships: " + 
                                                   shipsSent + " <> " + shipsNeeded + ", " + error;
                log("#\t " + target + " attacked with " + shipsSent + " ships from " 
                           + fromSources + " sources");
                log("#\t " + (shipsNeeded - (target.owner() == Race.ALLY ? 0 : _extraShips))
                           + " ships on turn " + attackTurn + " was predicted");
            }
        }
    }
    
}
