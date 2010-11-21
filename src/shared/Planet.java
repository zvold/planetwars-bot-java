package shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import continuous.Adjacency;

import filters.OwnerFilter;
import filters.PlanetFilter;

public class Planet {

    int     _id;
    double  _x;
    double  _y;
    int     _ships;
    int     _growth;
    Race    _owner;
    Object  _data;
    
    @SuppressWarnings("unchecked")
    ArrayList<Fleet>[] _incoming = new ArrayList[] {null, // no neutral fleets
                                                    new ArrayList<Fleet>(),
                                                    new ArrayList<Fleet>()};

    public Map<Integer, ArrayList<FutureOrder>> _orders = new HashMap<Integer, ArrayList<FutureOrder>>();
    
    // special constructor, should be used by Visialiser only
    public Planet(double x, double y, Race owner) {
        _id = 31;
        _x = x;
        _y = y;
        _growth = 1;
        _ships = 1;
        _owner = owner;
        _data = null;
    }
    public void setX(double x) {_x = x;}
    public void setY(double y) {_y = y;}
    
    private Planet(Planet from) {
        _id = from._id;
        _x = from._x;
        _y = from._y;
        _ships = from._ships;
        _growth = from._growth;
        _owner = from._owner;
        
        _incoming[Race.ALLY.ordinal()] = new ArrayList<Fleet>(from.incoming(Race.ALLY));
        _incoming[Race.ENEMY.ordinal()] = new ArrayList<Fleet>(from.incoming(Race.ENEMY));
        _orders = new HashMap<Integer, ArrayList<FutureOrder>>(from._orders.size());
        for (Integer key : from._orders.keySet())
            _orders.put(key, new ArrayList<FutureOrder>(from._orders.get(key)));
    }
    
    public Planet(int id, Matcher m) {
        assert(m.groupCount() == 5) : "check planet pattern group count";
        _id = id;
        _x = Double.parseDouble(m.group(1));
        _y = Double.parseDouble(m.group(2));
        _owner = Race.values()[Integer.parseInt(m.group(3))];
        _ships = Integer.parseInt(m.group(4));
        _growth = Integer.parseInt(m.group(5));
    }

    public void update(Matcher m) {
        assert(m.groupCount() == 5) : "check planet pattern group count";
        _owner = Race.values()[Integer.parseInt(m.group(3))];
        _ships = Integer.parseInt(m.group(4));
        
        assert(_x == Double.parseDouble(m.group(1))) : "check x coord invariant";
        assert(_y == Double.parseDouble(m.group(2))) : "check y coord invariant";
        assert(_growth == Integer.parseInt(m.group(5))) : "check growth rate invariant";
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Planet " + _id);
        b.append(" (" + _ships + ":" + _owner);
        b.append(" +" + _growth + ")");
        return b.toString();
    }

    public double distSquared(Planet that) {
        return (_x - that._x) * (_x - that._x) +
               (_y - that._y) * (_y - that._y);
    }
    
    public int distance(Planet that) {
        return DistanceCache.distance(this, that);
    }

    public void clearFleets() {
        _incoming[Race.ALLY.ordinal()].clear();
        _incoming[Race.ENEMY.ordinal()].clear();
    }

    public void addIncomingFleet(Fleet fleet) {
        _incoming[fleet.owner().ordinal()].add(fleet);
    }

    public ArrayList<Fleet> incoming(Race owner) {
        return _incoming[owner.ordinal()];
    }

    public void addFutureOrder(FutureOrder order) {
        if (!_orders.containsKey(order.turn()))
            _orders.put(order.turn(), new ArrayList<FutureOrder>());
        _orders.get(order.turn()).add(order);
        assert(checkOwnFutureOrders()) : "check future orders validity";
    }
    
    private boolean checkOwnFutureOrders() {
        for (Integer turn : _orders.keySet())
            for (FutureOrder order : _orders.get(turn))
                if (order.from() != id() && order.to() != id())
                    return false;
        return true;
    }

    public boolean hasFutureOrders(int turn) {
        return _orders.containsKey(turn) && !_orders.get(turn).isEmpty();
    }
    
    public Collection<Integer> futureTurns() {
        return _orders.keySet();
    }
    
    public Collection<FutureOrder> futureOrders(int turn) {
        return _orders.get(turn);
    }

    public void advanceFutureOrders() {
        Map<Integer, ArrayList<FutureOrder>> newMap 
            = new HashMap<Integer, ArrayList<FutureOrder>>(_orders.size());
        for (Integer turn : _orders.keySet()) {
            assert(turn >= 0) : "future or now";
            if (turn != 0)
                newMap.put(turn - 1, _orders.get(turn));
        }
        _orders = newMap;
    }
    
    public void clearFutureOrders() {
        _orders.clear();
    }

    public void removeFutureOrders(int i) {
        _orders.remove(i);
    }
    
    public Planet deepCopy() {
        return new Planet(this);
    }
    
    public Race owner() {
        return _owner;
    }

    public int ships() {
        return _ships;
    }

    public int growth() {
        return _growth;
    }

    public int id() {
        return _id;
    }

    public double x() {
        return _x;
    }

    public double y() {
        return _y;
    }

    public void addShips(int num) {
        _ships += num;
        assert(_ships >= 0) : "ships >= 0";
    }
    
    public void setShips(int ships) {
        _ships = ships;
        assert(_ships >= 0) : "ships >= 0";
    }

    public void setOwner(Race owner) {
        _owner = owner;
    }

    public boolean hasFutureOrders() {
        return !_orders.isEmpty();
    }

    public void advanceFleets() {
        advanceFleets(Race.ALLY);
        advanceFleets(Race.ENEMY);
    }

    private void advanceFleets(Race owner) {
        if (incoming(owner).isEmpty())
            return;
        ArrayList<Fleet> newIncoming = new ArrayList<Fleet>();
        for (Fleet fleet : incoming(owner)) {
            Fleet newFleet = fleet.getAdvanced();
            if (newFleet != null)
                newIncoming.add(newFleet);
        }
        _incoming[owner.ordinal()] = newIncoming;
    }

    public <K> void setData(K data) {
        _data = data;
    }
    
    @SuppressWarnings("unchecked")
    public <K> K data() {
        return (K)_data;
    }

    public void removeOutgoingFutureOrders() {
        for (Integer key : _orders.keySet()) {
            Set<FutureOrder> toRemove = new HashSet<FutureOrder>();
            for (FutureOrder order : _orders.get(key))
                if (order.from() == id())
                    toRemove.add(order);
            _orders.get(key).removeAll(toRemove);
        }
    }
    
    public boolean deepEquals(Planet that) {
        boolean ret = _x == that._x && _y == that._y &&
                      _ships == that._ships && _owner == that._owner &&
                      _growth == that._growth && _id == that._id;
        if (!ret)
            return ret;

        ret = ret && _incoming[Race.ALLY.ordinal() ].equals(that._incoming[Race.ALLY.ordinal() ]);
        ret = ret && _incoming[Race.ENEMY.ordinal()].equals(that._incoming[Race.ENEMY.ordinal()]);
        if (!ret)
            return ret;
        
        return _orders.equals(that._orders);
    }

    public FutureOrder removeFutureOrder(FutureOrder order) {
        FutureOrder found = null;
        int turnFound = -1;
        for (Integer turn : _orders.keySet()) {
            for (FutureOrder o : _orders.get(turn))
                if (o.id() == order.id()) {
                    found = o;
                    turnFound = turn;
                    break;
                }
        }
        assert(found != null) : "must have";
        futureOrders(turnFound).remove(found);
        return found;
    }
    
    public List<Planet> getNearestInRadius(List<Planet> neighbors, Race owner, final int radius) {
        List<Planet> ret = new ArrayList<Planet>();
        List<Planet> allies = new OwnerFilter(neighbors, owner).select();

        if (allies.isEmpty())
            return ret;
        final Planet source = allies.remove(0);
        final int distance = this.distance(source);
        final Planet target = this;
        ret.add(source);
        ret.addAll(new PlanetFilter(allies) {
            @Override
            public boolean filter(Planet planet) {
                assert (planet.distance(target) >= distance) : "check sorting"
                        + planet.distance(target) + " >= " + distance;
                return planet.distance(target) < 15 ||
                       (Math.abs(planet.distance(target) - distance) < radius);
            }
        }.select());
        assert (!ret.contains(target)) : "target doesn't belong to neighbors set";
        return ret;
    }    

    public List<Planet> selectCloserThan(final Race owner, final Adjacency adj, final int distance) {
        final Planet target = this;
        List<Planet> ret = new PlanetFilter(adj.neighbors(target)) {
            @Override
            public boolean filter(Planet planet) {
                if (planet.distance(target) > distance)
                    return false;
                return (planet.owner() == owner);
            }
        }.select(); 
        return ret;
    }
    
}
