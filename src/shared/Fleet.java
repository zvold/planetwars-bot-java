package shared;

import java.util.regex.Matcher;

import orders.Order;
import orders.OrderFactory;

public class Fleet {

    Race    _owner;
    Order   _order;
    int     _trip;
    int     _eta;
    int     _id;

    /**
     * Constructor used by Parser.
     * @param id Parser knows what this fleet id is.
     * @param m
     */
    public Fleet(int id, Matcher m) {
        assert(m.groupCount() == 6) : "check fleet matcher groups count";
        _id = id;
        update(m);
    }

    /**
     * Convenience constructor for eager updating of the game state. When an
     * order is issued, we can create an incoming fleet early.
     */
    public Fleet(Race owner, int ships, Planet src, Planet dst) {
        _owner = owner;
        _order = OrderFactory.createOrder(src.id(), dst.id(), ships);
        // trip and ETA are the same, since it has just departed
        _trip = src.distance(dst);
        _eta = _trip;
        _id = -1;
        assert(_eta != 0) : "ETA sanity check";
    }
    
    /**
     * Incomplete constructor, used by Simulator.
     * @param owner
     * @param ships
     */
    public Fleet(Race owner, int ships) {
        _owner = owner;
        _order = OrderFactory.createOrder(0, 0, ships);
    }

    Fleet(Fleet from) {
        _owner = from._owner;
        _order = from._order;
        _trip = from._trip;
        _eta = from._eta;
        _id = from._id;
        assert(_eta != 0) : "ETA sanity check";
    }
    
    Fleet getAdvanced() {
        if (_eta == 1)
            return null;
        Fleet ret = new Fleet(this);
        ret._eta--;
        return ret;
    }
    
    /**
     * Convenience method for updating of an existing Fleet object w/o creating
     * a new one. Used by Parser.
     * 
     * @param m
     */
    public void update(Matcher m) {
        _owner  = Race.values()[Integer.parseInt(m.group(1))];
        int ships  = Integer.parseInt(m.group(2));
        int src    = Integer.parseInt(m.group(3));
        int dst    = Integer.parseInt(m.group(4));
        _order = OrderFactory.createOrder(src, dst, ships);
        _trip   = Integer.parseInt(m.group(5));
        _eta    = Integer.parseInt(m.group(6));
        assert(_eta != 0) : "ETA sanity check";
    }
    
    @Override
    public String toString() {
        return "Fleet " + _order._src + "->" + _order._dst + 
               " (" + _order._shp + ":" + _owner + 
               ", ETA " + _eta + "/" + _trip + ")";
    }

    public int src() {
        return _order._src;
    }

    public int dst() {
        return _order._dst;
    }

    public Race owner() {
        return _owner;
    }

    public int ships() {
        return _order._shp;
    }

    public int eta() {
        return _eta;
    }

    public Fleet addShips(int ships) {
        _order = OrderFactory.createOrder(src(), dst(), ships() + ships);
        assert(ships() >= 0) : "ships >= 0";
        return this;
    }

}
