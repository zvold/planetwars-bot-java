package bot.algorithms;

import static bot.SimulatorBot.TURNS_PREDICT;
import shared.Planet;
import shared.Race;
import shared.Strategy;
import bot.SimulatorBot;

public class AdjustStrategy extends SimulatorBot.Algorithm {

    private static final int    STRATEGY_THRESH     = 10;
    
    Strategy            _strategy = Strategy.DEFENCE;
    int                 _agr;
    
    public AdjustStrategy(SimulatorBot bot, Strategy strategy, int agr) {
        bot.super(bot);
        _agr = agr;
        _strategy = strategy;
    }
    
    @Override
    public boolean execute() {
        int enemyShips = 0;
        int enemyGrowth = 0;
        int allyShips = 0;
        int allyGrowth = 0;
        for (Planet planet : bot()._game.planets()) {
            Planet future = bot()._sim.simulate(planet, TURNS_PREDICT);
            if (future.owner() == Race.ALLY) {
                allyShips += future.ships();
                allyGrowth += future.growth();
            } else if (future.owner() == Race.ENEMY) {
                enemyShips += future.ships();
                enemyGrowth += future.growth();
            }
        }
        
        _agr += (enemyGrowth >= allyGrowth ? 1 : -1);
        if (_agr >= STRATEGY_THRESH) {
            _agr = STRATEGY_THRESH;
            _strategy = Strategy.ATTACK;
        }
        else if (_agr <= -STRATEGY_THRESH) {
            _agr = -STRATEGY_THRESH;
            _strategy = Strategy.DEFENCE;
        }
        log("# strategy adjustment finished at " + bot()._timer.totalTime() + " ms total");
        return true;
    }

    public Strategy current() {
        return _strategy;
    }
    
}
