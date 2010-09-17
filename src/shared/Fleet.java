package shared;

import java.util.regex.Matcher;

public class Fleet {

    Race _owner;
    int _ships;
    int _src;
    int _dst;
    int _trip;
    int _eta;
    int _id;
    
    public Fleet(Race owner) {
        _owner = owner;
    }
    
    public Fleet(int id, Matcher m) {
        assert(m.groupCount() == 6) : "check fleet matcher groups count";
        _id = id;
        update(m);
    }

    public Fleet(Race owner, int ships) {
        this(owner);
        _ships = ships;
    }

    public Fleet(Race owner, int ships, Planet src, Planet dst) {
        _owner = owner;
        _ships = ships;
        _src = src.id();
        _dst = dst.id();
        _trip = src.distance(dst);
        _eta = _trip;
        _id = -1;
    }

    public void update(Matcher m) {
        _owner  = Race.values()[Integer.parseInt(m.group(1))];
        _ships  = Integer.parseInt(m.group(2));
        _src    = Integer.parseInt(m.group(3));
        _dst    = Integer.parseInt(m.group(4));
        _trip   = Integer.parseInt(m.group(5));
        _eta    = Integer.parseInt(m.group(6));
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Fleet " + _src + "->" + _dst);
        b.append(" (" + _ships + ":" + _owner + ", ETA " + _eta);
        b.append("/" + _trip + ")");
        return b.toString();
        
    }

    public int src() {
        return _src;
    }

    public int dst() {
        return _dst;
    }

    public Race owner() {
        return _owner;
    }

    public int ships() {
        return _ships;
    }

    public int eta() {
        return _eta;
    }

    public void setShips(int ships) {
        _ships = ships;
        assert(_ships >= 0) : "ships >= 0";
    }

    public Fleet addShips(int ships) {
        _ships += ships;
        assert(_ships >= 0) : "ships >= 0";
        return this;
    }

    public void setOwner(Race owner) {
        _owner = owner;
    }

}
