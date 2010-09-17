package simulate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import shared.Fleet;
import shared.FutureOrder;
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
        
        resetAllListeners();
        
        notifyPossibleMinShips(0, ret.owner(), ret.ships());
        
        for (int turn = 1; turn <= turns; turn++) {
            // departures
            if (ret.hasFutureOrders(turn)) {
                for (FutureOrder order : ret.futureOrders(turn)) {
                    if (order.from() == planet) {
                        if (ret.ships() >= order.ships() && order.owner() == planet.owner()) {
                            notifyPossibleMinShips(turn, ret.owner(), ret.ships() - order.ships());
                            ret.addShips(-order.ships());
                        }
                        else {
                            // invalid order during simulation, skip for now
                        }
                    }
                }
            }
            
            // growth ships
            if (ret.owner() != Race.NEUTRAL)
                ret.addShips(ret.growth());
            
            // number of ships arriving this turn
            _arriving.clear();
            
            if ((sumFleets = sumFleets(turn, ret, Race.ALLY)) != null)
                _arriving.add(sumFleets);
            if ((sumFleets = sumFleets(turn, ret, Race.ENEMY)) != null)
                _arriving.add(sumFleets);
            
            if (ret.owner() == Race.NEUTRAL)
                _arriving.add(new Fleet(Race.NEUTRAL).addShips(ret.ships()));
            
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
            } else if (!_arriving.isEmpty()) {
                Fleet fleet = _arriving.get(0);
                assert(fleet.owner() == ret.owner()) : "1 fleet - must be the same owner";
                if (fleet.owner() != Race.NEUTRAL)
                    ret.setShips(fleet.ships()); // old ships already added
            }
            notifyPossibleMinShips(turn, ret.owner(), ret.ships());
        }
        return ret;
    }

    private void resetMinShipListeners() {
        if (_minShipsListeners != null)
            for (MinShipsListener listener : _minShipsListeners)
                listener.reset();
    }

    private void resetAllListeners() {
        resetMinShipListeners();
        if (_ownerChangeListeners != null)
            for (OwnerChangeListener listener : _ownerChangeListeners)
                listener.reset();
    }

    private Fleet sumFleets(int turn, Planet planet, Race owner) {
        // planet's ships are added to the corresponding fleet
        Fleet ret = planet.owner() == owner ? 
                    new Fleet(owner, planet.ships()) : null;
        
        for (Fleet f : planet.incoming(owner))
            if (f.eta() == turn) {
                if (ret == null)
                    ret = new Fleet(owner);
                ret.addShips(f.ships());
            }
        
        // optimistically assume none of the incoming future orders were cancelled
        if (planet.hasFutureOrders(turn))
            for (FutureOrder order : planet.futureOrders(turn))
                if (order.to().id() == planet.id() && order.owner() == owner) {
                    if (ret == null)
                        ret = new Fleet(owner);
                    ret.addShips(order.ships());
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
