package shared;

import static shared.Race.ALLY;
import static shared.Race.ENEMY;

import java.util.ArrayList;
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

    private Planet(Planet from) {
        _id = from._id;
        _x = from._x;
        _y = from._y;
        _ships = from._ships;
        _growth = from._growth;
        _distCache = from._distCache;
        _owner = from._owner;
        
        _incoming[ALLY.ordinal()] = new ArrayList<Fleet>(from.incoming(ALLY));
        _incoming[ENEMY.ordinal()] = new ArrayList<Fleet>(from.incoming(ENEMY));
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
        _incoming[ALLY.ordinal()].clear();
        _incoming[ENEMY.ordinal()].clear();
    }

    public void addIncomingFleet(Fleet fleet) {
        _incoming[fleet.owner().ordinal()].add(fleet);
    }

    public ArrayList<Fleet> incoming(Race owner) {
        return _incoming[owner.ordinal()];
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

    public void setShips(int ships) {
        _ships = ships;
    }

    public void setOwner(Race owner) {
        _owner = owner;
    }

}
