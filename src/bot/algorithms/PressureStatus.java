package bot.algorithms;

import java.util.ArrayList;
import java.util.List;

import shared.Planet;
import shared.Race;
import bot.SimulatorBot;
import bot.SimulatorBot.Algorithm;

public class PressureStatus extends Algorithm {

    List<Planet> _canHold;
    
    public PressureStatus(SimulatorBot bot) {
        bot.super(bot);
    }

    @Override
    public boolean execute() {
        _canHold = new ArrayList<Planet>();
        calcPressure(Race.NEUTRAL);
        calcPressure(Race.ENEMY);
        
        log("# PressureStatus(): can hold " + _canHold.size() + " planets");
        log("#\t pressure status finished at " + bot()._timer.total() + " ms total");
        return true;
    }

    private void calcPressure(Race owner) {
        for (Planet planet : bot()._game.planets(owner)) {
            Pressure pressure = new Pressure(bot(), planet, Race.ALLY);
            pressure.execute();
            if (pressure.canHold())
                _canHold.add(planet);
        }
    }

    public boolean canHold(Planet planet) {
        return _canHold.contains(planet);
    }
    
}
