package bot.algorithms;

import java.util.List;
import java.util.Map;

import shared.FutureOrder;
import shared.Planet;
import shared.Race;
import simulate.ShipsGraphListener;
import bot.SimulatorBot;


public class CoordinatedAttack extends SimulatorBot.Algorithm {

    Planet                          _target;
    List<Planet>                    _sources;
    int                             _extraShips;
    Map<Planet, ShipsGraphListener> _avail;
    int                             _minTurn;
    
    public CoordinatedAttack(SimulatorBot bot, Planet target, 
                             List<Planet> sources,
                             int wait, Map<Planet, ShipsGraphListener> avail,
                             int extraShips) {
        bot.super(bot);
        _target = target;
        _sources = sources;
        _extraShips = extraShips;
        _avail = avail;
        _minTurn = wait;
    }

    @Override
    public boolean execute() {
        log("# CoordinatedAttack at " + _target + ", " + _sources.size() + " sources");
        
        int furthestSource = bot().getFurthest(_target, _sources);        
        
        Planet future = bot()._sim.simulate(_target, furthestSource + _minTurn);
        assert(future.owner() != Race.ALLY) : "wrong target for CoordinatedAttack";
        int shipsNeeded = future.ships() + _extraShips;
        
        bot()._game.clearAllData();
        int totalShips = 0;
        for (Planet src : _sources) {
            int ships = _avail.get(src).shipsAvail(_minTurn + furthestSource - src.distance(_target));
            ships /= 2;
            totalShips += ships;
            src.setData(ships);
        }
        
        int shipsSent = 0;
        int fromSources = 0;
        double error = 0.0;
        for (Planet src : _sources) {
            int shipsAvail = (Integer)src.data();
            double frac = (double)shipsNeeded * (double)shipsAvail / (double)totalShips;
            int num = (int)Math.floor(frac);
            error += frac - (double)num;
            if (error > 0.9999) {
                error -= 0.9999;
                num++;
            }
            int allyETA = src.distance(_target);          // allied ETA
            assert(num <= _avail.get(src).shipsAvail(_minTurn + furthestSource - allyETA) / 2) : 
                "calc correctness: " + num + " <= " + src.ships();
            if (num == 0) // don't bother
                continue;
            if (allyETA == furthestSource + _minTurn) {
                issueOrder(src, _target, num);
                src.addShips(-num);
            } else if (allyETA < furthestSource + _minTurn) {
                FutureOrder order = new FutureOrder(Race.ALLY, src, _target, num, _minTurn + furthestSource - allyETA);
                log("#\t future order " + order + " created");
                bot()._game.addFutureOrder(order);
            } else
                assert(false) : "source selected is too far away"; 
            shipsSent += num;
            fromSources++;
        }
        assert(shipsSent == shipsNeeded) : 
            "correct number of ships: " + shipsSent + " <> " + shipsNeeded + ", " + error;
        log("#\t " + _target + " attacked with " + shipsSent + " ships from " + fromSources + " sources");
        log("#\t " + (shipsNeeded - _extraShips) + " ships on turn " + furthestSource + " was predicted");
        
        return true;
    }

}
