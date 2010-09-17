package simulate;

import shared.Planet;

public interface EndTurnListener {

    public void endTurn(int turn, Planet planet);
    
    public void reset();

    public void reset(int turns);
    
}
