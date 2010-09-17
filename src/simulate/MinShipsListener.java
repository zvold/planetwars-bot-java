package simulate;

import shared.Race;

public class MinShipsListener {

    int[] _minShips = new int[Race.values().length];
    int[] _minTurns = new int[Race.values().length];
    
    public MinShipsListener() {
        reset();
    }
    
    public void reset() {
        for (int i=0; i<_minShips.length; i++) {
            _minShips[i] = Integer.MAX_VALUE;
            _minTurns[i] = -1;
        }
    }
    
    void notifyPossibleMinShips(int turn, Race owner, int ships) {
        // let's give preference to the minimum encountered earlier
        if (ships < _minShips[owner.ordinal()]) {
            _minShips[owner.ordinal()] = ships;
            _minTurns[owner.ordinal()] = turn;
            notifyMinShips(turn, owner, ships);
        }
    }
    
    public void notifyMinShips(int turn, Race owner, int ships) {
    }
    
    public int ships(Race owner) {
        return _minShips[owner.ordinal()] != Integer.MAX_VALUE ? 
               _minShips[owner.ordinal()] : 0 ; 
    }
    
    public int turn(Race owner) {
        return _minTurns[owner.ordinal()];
    }
    
    public boolean wasOwned(Race owner) {
        return _minShips[owner.ordinal()] != Integer.MAX_VALUE;
    }
}
