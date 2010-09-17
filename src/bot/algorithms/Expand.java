package bot.algorithms;

import java.util.Collections;
import java.util.List;

import shared.Planet;
import shared.Utils;
import bot.SimulatorBot;

import compare.IScore;

public class Expand extends SimulatorBot.Algorithm {
    
    Planet            _target;
    boolean           _safe;
    int               _extraShips;
    IScore<Planet>    _closeness;
    List<Planet>      _expand;
    
    public Expand(SimulatorBot bot, IScore<Planet> closeness, List<Planet> expandTo, 
                  int extraShips, boolean safe) {
        bot.super(bot);
        _safe = safe;
        _extraShips = extraShips;
        _closeness = closeness;
        _expand = expandTo;
    }
    
    @Override
    public boolean execute() {
        Collections.sort(_expand, _closeness);
        
        if (_expand.size() > 1)
            _expand = _expand.subList(0, _expand.size() / 2);
        
        while (bot()._timer.total() < Utils.timeout() - 50 && !_expand.isEmpty()) {
            Planet target = _expand.remove(0);
            new WaitingAttack(bot(), target, _extraShips, 0, _safe).execute();
        }

        log("# expand() finished at " + bot()._timer.total() + " ms total");
        return true;
    }

}
