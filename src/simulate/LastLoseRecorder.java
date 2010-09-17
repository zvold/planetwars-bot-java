package simulate;

import shared.Race;

public class LastLoseRecorder implements OwnerChangeListener {

    Race _changedFrom;
    Race _changedTo;
    int _turn = -1;
    int _ships = -1;
    
    public LastLoseRecorder(Race changedTo) {
        _changedTo = changedTo;
    }
    
    public LastLoseRecorder(Race changedFrom, Race changedTo) {
        _changedFrom = changedFrom;
        _changedTo = changedTo;
    }

    @Override
    public void ownerChanged(int turn, Race fromRace, int fromShips,
                                       Race toRace, int toShips) {
        fromRace = (_changedFrom == null ? null : fromRace);        
        if (toRace == _changedTo && fromRace == _changedFrom) {
            _ships = toShips;
            _turn = turn;
        }
    }

    @Override
    public void reset() {
        _turn = -1;
        _ships = -1;
    }

    public boolean lost() {
        return (_turn != -1);
    }
    
    public int turn() {
        return _turn;
    }
    
    public int ships() {
        return _ships;
    }
    
}
