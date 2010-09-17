package bot.algorithms;

import shared.Race;
import bot.SimulatorBot;

public class Profile extends SimulatorBot.Algorithm{

    private static final int NUM_SAMPLES = 15;
    
    int[][] _growth = new int[NUM_SAMPLES][Race.values().length];
    int[][] _ships  = new int[NUM_SAMPLES][Race.values().length];
    int[][] _fleets = new int[NUM_SAMPLES][Race.values().length];
    int _cur;
    float[] _avgGrowth = new float[Race.values().length];
    float[] _avgShips  = new float[Race.values().length];
    float[] _avgFleets = new float[Race.values().length];
    
    public Profile(SimulatorBot bot) {
        bot.super(bot);
    }
    
    @Override
    public boolean execute() {
        _growth[_cur][Race.ENEMY.ordinal()] = bot()._game.growth(Race.ENEMY);
        _growth[_cur][Race.ALLY.ordinal() ] = bot()._game.growth(Race.ALLY);
        _ships [_cur][Race.ENEMY.ordinal()] = bot()._game.ships(Race.ENEMY);
        _ships [_cur][Race.ALLY.ordinal() ] = bot()._game.ships(Race.ALLY);
        _fleets[_cur][Race.ENEMY.ordinal()] = bot()._game.fleets(Race.ENEMY);
        _fleets[_cur][Race.ALLY.ordinal() ] = bot()._game.fleets(Race.ALLY);
        if (++_cur == NUM_SAMPLES)
            _cur = 0;

        average();
        return true;
    }

    private void average() {
        averageGrowth(Race.ENEMY);
        averageGrowth(Race.ALLY);
        averageShips(Race.ENEMY);
        averageShips(Race.ALLY);
        averageFleets(Race.ENEMY);
        averageFleets(Race.ALLY);
    }

    private void averageFleets(Race owner) {
        _avgFleets[owner.ordinal()] = 0;        
        for (int i=0; i<NUM_SAMPLES; i++) {
            _avgFleets[owner.ordinal()] += _fleets[i][owner.ordinal()];
        }
        _avgFleets[owner.ordinal()] /= (float)NUM_SAMPLES;
    }

    private void averageGrowth(Race owner) {
        _avgGrowth[owner.ordinal()] = 0;        
        for (int i=0; i<NUM_SAMPLES; i++) {
            _avgGrowth[owner.ordinal()] += _growth[i][owner.ordinal()];
        }
        _avgGrowth[owner.ordinal()] /= (float)NUM_SAMPLES;
    }

    private void averageShips(Race owner) {
        _avgShips[owner.ordinal()] = 0;        
        for (int i=0; i<NUM_SAMPLES; i++) {
            _avgShips[owner.ordinal()] += _ships[i][owner.ordinal()];
        }
        _avgShips[owner.ordinal()] /= (float)NUM_SAMPLES;
    }
    
    public float ships(Race owner) {
        assert(owner != Race.NEUTRAL) : "no neutral";
        return _avgShips[owner.ordinal()];
    }

    public float growth(Race owner) {
        assert(owner != Race.NEUTRAL) : "no neutral";
        return _avgGrowth[owner.ordinal()];
    }
    
    public float fleets(Race owner) {
        assert(owner != Race.NEUTRAL) : "no neutral";
        return _avgFleets[owner.ordinal()];
    }
    
}
