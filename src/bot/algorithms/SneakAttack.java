package bot.algorithms;

import static bot.SimulatorBot.TURNS_PREDICT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import shared.FutureOrder;
import shared.Planet;
import shared.Race;
import shared.Utils;
import bot.SimulatorBot;

import compare.IScore;

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
        // first, try to defend allied planets that we lose to the enemy
        sneakDefence();
        log("# sneakDefence() finished at " + bot()._timer.totalTime() + " ms total");
        
        // second, try to sneakily intercept neutral planets transferred to enemy
        sneakAttack();
        log("# sneakAttack() finished at " + bot()._timer.totalTime() + " ms total");
        
        return true;
    }

    private void sneakAttack() {
        // select planets having neutral->enemy transition and remaining with enemy
        List<Planet> attack = new ArrayList<Planet>();
        List<Planet> planets = bot()._game.planets();
        for (Planet planet : planets) {
            Planet future = bot()._sim.simulate(planet, TURNS_PREDICT);
            if (future.owner() == Race.ENEMY && bot()._lastN2ELostListener.lost())
                attack.add(planet);
        }
        Collections.sort(attack, _closeness);
        if (attack.size() > 1)
            attack = attack.subList(0, attack.size() / 2);

        sneakAttack(attack);
    }

    private void sneakDefence() {
        // select planets having ally->enemy transition and remaining with enemy
        ArrayList<Planet> attack = new ArrayList<Planet>();
        for (Planet planet : bot()._game.planets()) {
            Planet future = bot()._sim.simulate(planet, TURNS_PREDICT);
            if (future.owner() == Race.ENEMY && bot()._firstA2ELostListener.lost())
                attack.add(planet);
        }
        Collections.sort(attack, _closeness);
        sneakAttack(attack);
    }

    private void sneakAttack(List<Planet> attack) {
        log("# " + attack.size() + " targets selected at " + bot()._timer.totalTime() + " ms total");
        
        while (bot()._timer.totalTime() < (Utils.timeout() - 50) && !attack.isEmpty()) {
            // get best potential planet to attack
            Planet target = attack.remove(0);

            // simulate to determine best turn to attack
            int shipsNeeded = 0;
            int attackTurn = 0;
            Planet future = bot()._sim.simulate(target, TURNS_PREDICT);
            if (future.owner() == Race.ENEMY) {
                if (bot()._firstA2ELostListener.lost()) {
                    attackTurn = bot()._firstA2ELostListener.turn();
                    future = bot()._sim.simulate(target, attackTurn);
                    shipsNeeded = future.ships();
                } else if (bot()._lastN2ELostListener.lost()) {
                    attackTurn = bot()._lastN2ELostListener.turn() + 1;
                    future = bot()._sim.simulate(target, attackTurn);
                    shipsNeeded = future.ships() + _extraShips;
                } else
                    assert(false) : "sneakAttack() was called for invalid planet";
            }

//            List<Planet> sources = selectCloserThan(Race.ALLY, target, attackTurn);
            List<Planet> sources = bot().selectOwnerToSum(Race.ALLY, target, 
                                                          (int)(shipsNeeded * SNEAK_SHIPS_FACTOR), 
                                                          attackTurn);
            
            int totalShips = 0;
            int maxAvail = 0;        // for "1 ship" correction
            Planet maxPlanet = null; // for "1 ship" correction
            int shipsAvail;
            for (Planet src : sources) {
                // TODO: simulate can be for (attackTurn - src.distance(target)) turns ?
                if ((shipsAvail = bot().shipsAvailable(src, Race.ALLY)) == 0)
                    continue;
                totalShips += shipsAvail;
                if (shipsAvail > maxAvail) {
                    maxAvail = shipsAvail;
                    maxPlanet = src;
                }
            }
            
            if (totalShips == 0)
                continue;
            assert(maxPlanet != null) : "we have sources";
            
            if (totalShips >= shipsNeeded) {
                int shipsSent = 0;
                int fromSources = 0;
                for (Planet src : sources) {
                    // TODO: simulate can be for (attackTurn - src.distance(target)) turns ?
                    if ((shipsAvail = bot().shipsAvailable(src, Race.ALLY)) == 0)
                        continue;
                    int num = (int)(0.5d + (double)shipsNeeded *
                                           (double)shipsAvail / (double)totalShips);
                    assert(num <= src.ships()) : "calc correctness";
                    if (num == 0) // don't bother
                        continue;
                    if (shipsSent + num >= shipsNeeded)
                        num = (shipsNeeded - shipsSent);
                    int allyETA = src.distance(target);          // allied ETA
                    if (allyETA == attackTurn) {
                        issueOrder(src, target, num);
                        src.addShips(-num);
                    } else if (allyETA < attackTurn) {
                        FutureOrder order = new FutureOrder(src, target, num, attackTurn - allyETA);
                        log("# future order " + order + " created");
                        bot()._game.addFutureOrder(order);
                    } else
                        assert(false) : "source selected is too far away"; 
                    shipsSent += num;
                    fromSources++;
                }
                
                if (shipsSent < shipsNeeded) {
                    int need = shipsNeeded - shipsSent;
                    if (maxPlanet.ships() >= need) {
                        int allyETA = maxPlanet.distance(target);          // allied ETA
                        if (allyETA == attackTurn) {
                            issueOrder(maxPlanet, target, need);
                            maxPlanet.addShips(-need);
                            log("# " + need + " correction ships sent");                            
                        } else if (allyETA < attackTurn) {
                            FutureOrder order = new FutureOrder(maxPlanet, target, need, attackTurn - allyETA);
                            log("# future order " + order + " created (correction)");
                            bot()._game.addFutureOrder(order);
                        } else
                            assert(false) : "source selected is too far away"; 
                    }
                }
                
                log("# " + target + " attacked with " + shipsSent + " ships from " 
                         + fromSources + " sources");
                log("# " + (shipsNeeded - (target.owner() == Race.ALLY ? 0 : _extraShips))
                         + " ships on turn " + attackTurn + " was predicted");
            }
        }
    }
    
}
