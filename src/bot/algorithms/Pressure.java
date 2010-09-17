package bot.algorithms;

import shared.FutureOrder;
import shared.Planet;
import shared.Race;
import simulate.ShipsGraphListener;
import bot.SimulatorBot;

public class Pressure extends SimulatorBot.Algorithm {

    public static final int PREDICT_ATTACK = 15;
    
    Planet  _target;
    int     _shipsAvail;
    Planet  _future;
    Race    _owner;
    
    public Pressure(SimulatorBot bot, Planet planet, Race owner) {
        bot.super(bot);
        _target = planet.deepCopy();
        _owner = owner;
    }

    public void execute(int sentShips) {
        _target.addShips(-sentShips);
        execute();
    }
    
    @Override
    public boolean execute() {
        _shipsAvail = bot().shipsAvailable(_target, _owner);
        _target.removeOutgoingFutureOrders();
        
        imitateAttack(_owner.opponent(), false);
        imitateAttack(_owner, true);

        _future = bot()._sim.simulate(_target, SimulatorBot.TURNS_PREDICT);
        if (!bot()._ownerListener.changed()) {
            assert(_future.owner() == _target.owner()) : "owner change listener bug";
            int tmp = bot()._minShipsListener.ships(_owner);
            if (tmp < _shipsAvail)
                _shipsAvail = tmp;
        }
        return true;
    }

    public int shipsAvail() {
        return _shipsAvail;
    }
    
    public boolean canHold() {
        return _future.owner() == _owner;
    }
    
    private void imitateAttack(Race owner, boolean skipTurn) {
        ShipsGraphListener graph = new ShipsGraphListener(owner);
        bot()._sim.addListener(graph);

        // imitate immediate attack from all owner's planets
        for (Planet planet : bot()._game.planets(owner)) {
            if (planet.id() == _target.id())
                continue;
            
            bot()._sim.simulate(planet, SimulatorBot.TURNS_PREDICT);
            
            for (int turn=skipTurn ? 1 : 0; 
                 turn < PREDICT_ATTACK + (skipTurn ? 1 : 0); 
                 turn++) {
                int shipsAvail = graph.shipsAvail(turn);
                int shipsPrev = turn > 0 ? graph.shipsAvail(turn - 1) : 0;
                if (shipsPrev > 0 && shipsAvail > 0)
                    shipsAvail -= shipsPrev;
                assert(shipsAvail >= 0) : "sanity check";
                if (shipsAvail == 0)
                    continue;
                FutureOrder order = new FutureOrder(owner, planet, _target, 
                                                    shipsAvail, turn + _target.distance(planet));
                _target.addFutureOrder(order);
            }
        }
        
        bot()._sim.clearEndTurnListeners();
    }
    
}
