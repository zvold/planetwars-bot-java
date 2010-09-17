package simulate;

import shared.Race;

public class FirstLoseRecorder implements OwnerChangeListener {

    Race _changedFrom = null;
    Race _changedTo;
    int _turn = -1;
    int _ships = -1;
    
    public FirstLoseRecorder(Race changedTo) {
        _changedTo = changedTo;
    }

    public FirstLoseRecorder(Race changedFrom, Race changedTo) {
        _changedFrom = changedFrom;
        _changedTo = changedTo;
    }
    
    @Override
    public void ownerChanged(int turn, Race fromRace, int fromShips,
                                       Race toRace, int toShips) {
        fromRace = (_changedFrom == null ? null : fromRace);
        if (_turn == -1 && toRace == _changedTo
                        && fromRace == _changedFrom) {
            _ships = toShips;
            _turn = turn;
        }
    }

    @Override
    public void reset() {
        _turn = -1;
        _ships = -1;
    }

    @Override
    public boolean changed() {
        return (_turn != -1);
    }
    
    public int turn() {
        return _turn;
    }
    
    public int ships() {
        return _ships;
    }
    
}
