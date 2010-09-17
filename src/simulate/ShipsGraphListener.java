package simulate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import shared.Planet;
import shared.Race;
import bot.SimulatorBot;

public class ShipsGraphListener implements EndTurnListener {

    public static final int SIZE = SimulatorBot.TURNS_PREDICT + 1;
    
    List<Integer> _graph;
    List<Integer> _mins;
    Race          _owner;
    int           _size;
    boolean       _first = true;
    
    public ShipsGraphListener(Race owner) {
        _owner = owner;
        init(SIZE);
    }
    
    private void init(int size) {
        _size = size;
        _graph = new ArrayList<Integer>(size);
        _mins = new ArrayList<Integer>(size);
        for (int i=0; i<size; i++) {
            _graph.add(0);
            _mins.add(Integer.MAX_VALUE);
        }
    }

    @Override
    public void endTurn(int turn, Planet planet) {
        int sign = planet.owner() == _owner ? 1 : -1;
        if (planet.owner() == Race.NEUTRAL)
            sign = 0;
        assert(turn < _size) : "EndTurnListener size check";
        _graph.set(turn, planet.ships() * sign);
    }

    @Override
    public void reset() {
        Collections.fill(_graph, 0);
        Collections.fill(_mins, Integer.MAX_VALUE);
        _first = true;
    }

    public int shipsAvail(int turn) {
        assert(turn < _size) : "ShipsGraph simulated for " + _size + " turns, requested " + turn;
        if (_graph.isEmpty())
            return 0;
        if (_first)
            initMinimums();
        assert(_mins.get(turn) != Integer.MAX_VALUE) : "minimums validity";
        return _mins.get(turn) > 0 ? _mins  .get(turn) : 0;
    }

    private void initMinimums() {
        int idx = _size - 1;
        _mins.set(idx, _graph.get(idx));
        
        for (idx = _size - 2; idx>=0; idx--)
            _mins.set(idx, Math.min(_mins.get(idx+1), _graph.get(idx)));
        _first = false;
    }

    @Override
    public void reset(int turns) {
        if (_size <= turns + 1)
            init(turns + 1);
        else
            _size = turns + 1;
        reset();
    }
    
}
