package bot.algorithms;

import static bot.SimulatorBot.TURNS_PREDICT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import shared.Planet;
import shared.Race;
import shared.Utils;
import bot.SimulatorBot;

import compare.IScore;

public class Expand extends SimulatorBot.Algorithm {

    Planet            _target;
    boolean           _safe;
    int               _extraShips;
    Race              _owner;
    IScore<Planet>    _closeness;
    
    public Expand(SimulatorBot bot, IScore<Planet> closeness, Race owner, int extraShips, boolean safe) {
        bot.super(bot);
        _owner = owner;
        _safe = safe;
        _extraShips = extraShips;
        _closeness = closeness;
    }
    
    @Override
    public boolean execute() {
        List<Planet> expand = new ArrayList<Planet>();
        for (Planet planet : bot()._game.planets())
            if (planet.owner() == _owner) {
                Planet future = bot()._sim.simulate(planet, TURNS_PREDICT);
                if (future.owner() == _owner)
                    expand.add(planet);
            }
        Collections.sort(expand, _closeness);
        
        if (expand.size() > 1)
            expand = expand.subList(0, expand.size() / 2);
        
        while (bot()._timer.totalTime() < Utils.timeout() - 50 && !expand.isEmpty()) {
            Planet target = expand.remove(0);
            new WaitingAttack(bot(), target, _extraShips, 0, _safe).execute();
        }

        log("# expand() finished at " + bot()._timer.totalTime() + " ms total");
        return true;
    }

}
