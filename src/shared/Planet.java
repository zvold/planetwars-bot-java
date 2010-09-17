package shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

public class Planet {

    int     _id;
    double  _x;
    double  _y;
    int     _ships;
    int     _growth;
    Race    _owner;
    Map<Integer, Integer> _distCache = new HashMap<Integer, Integer>();
    
    @SuppressWarnings("unchecked")
    ArrayList<Fleet>[] _incoming = new ArrayList[] {null, // no neutral fleets
                                                    new ArrayList<Fleet>(),
                                                    new ArrayList<Fleet>()};

    Map<Integer, ArrayList<FutureOrder>> _orders = new HashMap<Integer, ArrayList<FutureOrder>>();
    
    private Planet(Planet from) {
        _id = from._id;
        _x = from._x;
        _y = from._y;
        _ships = from._ships;
        _growth = from._growth;
        _distCache = from._distCache;
        _owner = from._owner;
        
        _incoming[Race.ALLY.ordinal()] = new ArrayList<Fleet>(from.incoming(Race.ALLY));
        _incoming[Race.ENEMY.ordinal()] = new ArrayList<Fleet>(from.incoming(Race.ENEMY));
        _orders = new HashMap<Integer, ArrayList<FutureOrder>>(from._orders);
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
        int dist;
        if (_distCache.containsKey(that.id()))
            dist = _distCache.get(that.id());
        else {
            dist = (int)Math.ceil(Math.sqrt(distSquared(that)));
            _distCache.put(that.id(), dist);
        }
        return dist;
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
                if (order.from() != this && order.to() != this)
                    return false;
        return true;
    }

    public boolean hasFutureOrders(int turn) {
        return _orders.containsKey(turn);
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

}
