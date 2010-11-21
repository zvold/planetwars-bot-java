package simulate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import shared.Fleet;
import shared.FutureOrder;
import shared.Game;
import shared.Planet;
import shared.Race;
import shared.Utils;

public class Simulator {

    // descending order comparator
    public static final Comparator<Fleet> _comp = new Comparator<Fleet>() {
        @Override
        public int compare(Fleet f1, Fleet f2) {
            return f2.ships() - f1.ships();
        }
    }; 

    Set<OwnerChangeListener>    _ownerChangeListeners;
    Set<MinShipsListener>       _minShipsListeners;
    Set<EndTurnListener>        _endTurnListeners;
    ArrayList<Fleet>            _arriving = new ArrayList<Fleet>(3);
    boolean                     _failTolerant;
    boolean                     _wereCleaned;
    Game                        _game;
    
    public Simulator(Game game) {
        _game = game;
    }
    
    public Planet simulate(Planet planet, int turns, boolean failTolerant) {
        _failTolerant = failTolerant;
        _wereCleaned = false;
        Planet ret = simulate(planet, turns);
        _failTolerant = false;
        return ret;
    }
    
    public Planet simulate(Planet planet, int turns) {
        Fleet sumFleets;
        Planet ret = planet.deepCopy();
        
        resetAllListeners(turns);
        
        notifyPossibleMinShips(0, ret.owner(), ret.ships());
        notifyEndTurnPlanet(0, ret);
        
        for (int turn = 1; turn <= turns; turn++) {
            ret.advanceFutureOrders();
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
                _arriving.add(new Fleet(Race.NEUTRAL, ret.ships()));
            
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

            depart(planet, ret, 0);
            ret.advanceFleets();
            notifyPossibleMinShips(turn, ret.owner(), ret.ships());
            notifyEndTurnPlanet(turn, ret);
        }
        return ret;
    }

    private boolean depart(Planet orig, Planet copy, int turn) {
        boolean hadOrders = false;
        Set<FutureOrder> toRemoveSimulated = new HashSet<FutureOrder>();
        Set<FutureOrder> toRemoveFailing = new HashSet<FutureOrder>();
        // departures
        if (copy.hasFutureOrders(turn)) {
            for (FutureOrder order : copy.futureOrders(turn)) {
                if (order.from() == orig.id()) {
                    if (copy.ships() >= order.ships() && order.owner() == copy.owner()) {
                        notifyPossibleMinShips(turn, copy.owner(), copy.ships() - order.ships());
                        copy.addShips(-order.ships());
                        hadOrders = true;
                        toRemoveSimulated.add(order);
                    }
                    else {
                        if (!_failTolerant)
                            assert(reportInvalidFutureOrder(orig, copy, order)) : 
                                "can't have an invalid future order";
                        else {
                            _wereCleaned = true;
                            reportFailingOrder(order);
                            toRemoveFailing.add(order);
                        }
                    }
                }
            }
        }
        for (FutureOrder order : toRemoveFailing) {
                _game.removeArrival(order);
                orig.removeFutureOrder(order);            
        }            
        for (FutureOrder order : toRemoveSimulated) {
            copy.removeFutureOrder(order);
        }
            
        return hadOrders;
    }

    private void reportFailingOrder(FutureOrder order) {
        if (Utils._verbose)
            System.err.println("#\t cleaning " + order + ", source " + _game.planet(order.from()));
    }

    private boolean reportInvalidFutureOrder(Planet orig, Planet copy, FutureOrder order) {
        System.err.println("orig: " + orig);
        System.err.println("copy: " + copy);
        System.err.println(order);
        return false;
    }

    public void resetMinShipListeners() {
        if (_minShipsListeners != null)
            for (MinShipsListener listener : _minShipsListeners)
                listener.reset();
    }

    private void resetAllListeners(int turns) {
        resetMinShipListeners();
        if (_ownerChangeListeners != null)
            for (OwnerChangeListener listener : _ownerChangeListeners)
                listener.reset();
        if (_endTurnListeners != null)
            for (EndTurnListener listener : _endTurnListeners)
                listener.reset(turns);
    }

    private Fleet sumFleets(int turn, Planet planet, Race owner) {
        // planet's ships are added to the corresponding fleet
        Fleet ret = planet.owner() == owner ? 
                    new Fleet(owner, planet.ships()) : null;
        
        for (Fleet f : planet.incoming(owner))
            if (f.eta() == 1) {
                if (ret == null)
                    ret = new Fleet(owner, f.ships());
                else
                    ret.addShips(f.ships());
            }
        
        // optimistically assume none of the incoming future orders were cancelled
        if (planet.hasFutureOrders(0))
            for (FutureOrder order : planet.futureOrders(0))
                if (order.to() == planet.id() && order.owner() == owner) {
                    if (ret == null)
                        ret = new Fleet(owner, order.ships());
                    else
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

    private void notifyEndTurnPlanet(int turn, Planet planet) {
        if (_endTurnListeners == null)
            return;
        for (EndTurnListener listener : _endTurnListeners)
            listener.endTurn(turn, planet);
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

    public void addListener(EndTurnListener listener) {
        if (_endTurnListeners == null)
            _endTurnListeners = new HashSet<EndTurnListener>();
        _endTurnListeners.add(listener);
    }
    
    public void clearEndTurnListeners() {
        if (_endTurnListeners != null)
            _endTurnListeners.clear();
    }

    public boolean wereCleaned() {
        return _wereCleaned;
    }
}
