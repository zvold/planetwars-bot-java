package simulate;

import shared.Race;

public interface OwnerChangeListener {

    public void ownerChanged(int turn, Race fromRace, int fromShips, Race toRace, int toShips);
    
    public void reset();
    
    public int turn();
    
    public boolean changed();
    
}
