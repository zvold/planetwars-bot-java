package simulate;

import shared.Race;

public class FirstLoseRecorder implements OwnerChangeListener {

    Race _changedTo;
    int _turn = -1;
    int _ships = -1;
    
    public FirstLoseRecorder(Race changedTo) {
        _changedTo = changedTo;
    }
    
    @Override
    public void ownerChanged(int turn, Race fromRace, int fromShips,
                                       Race toRace, int toShips) {
        if (toRace == _changedTo) {
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
