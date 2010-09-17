package simulate;

import shared.Race;

public class OwnerChangeRecordListener implements OwnerChangeListener {

    boolean _ownerChanged;
    int     _turn;

    @Override
    public void ownerChanged(int turn, Race fromRace, int fromShips, Race toRace, int toShips) {
        _ownerChanged = true;
        // record last owner change turn
        if (!_ownerChanged)
            _turn = turn;
    }

    @Override
    public void reset() {
        _ownerChanged = false;
    }

    @Override
    public int turn() {
        return _turn;
    }

    @Override
    public boolean changed() {
        return _ownerChanged;
    }

}
