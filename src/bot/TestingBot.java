package bot;

import shared.Planet;
import shared.Race;
import bot.algorithms.Expand;
import bot.algorithms.FastExpand;
import bot.algorithms.Profile;
import bot.algorithms.Reacquire;
import bot.algorithms.Reinforce;
import bot.algorithms.SneakAttack;

import compare.CumulativeCloseness;
import compare.IScore;
import compare.SimpleCloseness;
import compare.SimpleGrowthCloseness;

public class TestingBot extends SimulatorBot {

    private static final int    EXTRA_SHIPS         = 1;

    IScore<Planet>      _allyCloseness;
    IScore<Planet>      _enemyCloseness;
    IScore<Planet>      _enemySimpleCloseness;
    IScore<Planet>      _cumulativeCloseness;
    
    // algorithms
    Profile             _profile;
    
    public static void main(String[] args) {
        parseArgs(args);
        SimulatorBot bot = new TestingBot();
        bot.run();
    }
    
    private TestingBot() {
        super();
        _profile = new Profile(this);
    }
    
    @Override
    public void doTurn() {
        super.doTurn();

        _allyCloseness = new SimpleGrowthCloseness(_game.planets(Race.ALLY));
        _enemyCloseness = new SimpleGrowthCloseness(_game.planets(Race.ENEMY));
        _enemySimpleCloseness = new SimpleCloseness(_game.planets(Race.ENEMY));
        _cumulativeCloseness = new CumulativeCloseness(_game.planets());
        
        if (_turn == 0 && new FastExpand(this).execute())
            return;
    
        // TODO: discard future orders that are valid but not necessary
        //carryOutFutureOrders();
        _game.clearFutureOrders();

        new SneakAttack(this, _allyCloseness, EXTRA_SHIPS).execute();

        new Reacquire(this, _allyCloseness, EXTRA_SHIPS).execute();

        _profile.execute();

        new Expand(this, _allyCloseness, Race.NEUTRAL, EXTRA_SHIPS, true).execute();
        new Expand(this, _allyCloseness, Race.ENEMY, EXTRA_SHIPS, false).execute();
        
        new Reinforce(this, _enemySimpleCloseness).execute();
    }
    
}
