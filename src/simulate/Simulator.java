package simulate;

import static shared.Race.ALLY;
import static shared.Race.ENEMY;
import static shared.Race.NEUTRAL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import shared.Fleet;
import shared.Planet;
import shared.Race;

public class Simulator {

    // descending order comparator
    public static final Comparator<Fleet> _comp = new Comparator<Fleet>() {
        @Override
        public int compare(Fleet f1, Fleet f2) {
            return f2.ships() - f1.ships();
        }
    }; 

    Set<OwnerChangeListener> _ownerChangeListeners;
    Set<MinShipsListener> _minShipsListeners;
    ArrayList<Fleet> _arriving = new ArrayList<Fleet>(3);
    
    public Planet simulate(Planet planet, int turns) {
        Fleet sumFleets;
        Planet ret = planet.deepCopy();
        
        notifyPossibleMinShips(0, ret.owner(), ret.ships());
        
        for (int turn = 1; turn <= turns; turn++) {
            // growth ships
            if (ret.owner() != NEUTRAL)
                ret.setShips(ret.ships() + ret.growth());
            
            // number of ships arriving this turn
            _arriving.clear();
            
            if ((sumFleets = sumFleets(turn, ret, ALLY)) != null)
                _arriving.add(sumFleets);
            if ((sumFleets = sumFleets(turn, ret, ENEMY)) != null)
                _arriving.add(sumFleets);
            
            // planet's ships are added to the corresponding fleet
            _arriving.add(new Fleet(ret.owner()).addShips(ret.ships()));
            
            if (_arriving.size() > 1) {
                Collections.sort(_arriving, _comp);
                Fleet max = _arriving.get(0);
                Fleet next = _arriving.get(1);

                if (max.ships() == next.ships()) {
                    ret.setShips(0);
                    // owner isn't changed if there are no survivors
                    //ret.setOwner(ret.owner());
                } else {
                    int oldShips = ret.ships();
                    Race oldOwner = ret.owner();
                    ret.setShips(max.ships() - next.ships());
                    ret.setOwner(max.owner());
                    notifyOwnerChange(turn, oldOwner, oldShips, ret.owner(), ret.ships());
                }
            }
            notifyPossibleMinShips(turn, ret.owner(), ret.ships());
        }
        
        return ret;
    }

    private Fleet sumFleets(int turn, Planet planet, Race owner) {
        Fleet ret = null;
        for (Fleet f : planet.incoming(owner))
            if (f.eta() == turn) {
                if (ret == null)
                    ret = new Fleet(owner);
                ret.addShips(f.ships());
            }
        return ret;
    }

    private void notifyPossibleMinShips(int turn, Race owner, int ships) {
        if (_minShipsListeners == null)
            return;
        for (MinShipsListener listener : _minShipsListeners)
            listener.notifyPossibleMinShips(turn, owner, ships);
    }
    
    private void notifyOwnerChange(int turn, Race fromRace, int fromShips, Race toRace, int toShips) {
        if (fromRace == toRace || _ownerChangeListeners == null)
            return;
        for (OwnerChangeListener listener : _ownerChangeListeners)
            listener.ownerChanged(turn, fromRace, fromShips, toRace, toShips);
    }

    public void addListener(OwnerChangeListener listener) {
        if (_ownerChangeListeners == null)
            _ownerChangeListeners = new HashSet<OwnerChangeListener>();
        _ownerChangeListeners.add(listener);
    }
    
    public void addListener(MinShipsListener listener) {
        if (_minShipsListeners == null)
            _minShipsListeners = new HashSet<MinShipsListener>();
        _minShipsListeners.add(listener);
    }
    
}
