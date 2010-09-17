package shared;

import orders.Order;
import orders.OrderFactory;

/**
 * Represents an order to be issued in the future. There are 2 copies of the
 * same order - one stored in departing planet, and one in arriving one. They
 * have the same order id.
 */
public class FutureOrder {

    int    _id;         // just an unique id, same for departing and arrival orders
    Order  _order;
    Race   _owner;
    int    _turn;       // initial turns at the order's creation time
    
    private static int _counter = 0;
    
    FutureOrder(int id, Race owner, Planet from, Planet to, int ships, int turn) {
        _order = OrderFactory.createOrder(from.id(), to.id(), ships);
        _turn = turn;
        _id = id;
        _owner = owner;
        assert(ships() >= 0) : "ships >= 0";
    }
    
    public FutureOrder(Race owner, Planet from, Planet to, int ships, int turn) {
        this(_counter++, owner, from, to, ships, turn);
    }
    
    @Override
    public String toString() {
        return "(" + _id + ") " + ships() + ":" + _owner + ", " +  
               from() + " -> " + to() + ", at " + _turn;
    }

    public Race owner() {
        return _owner;
    }
    
    public int id() {
        return _id;
    }
    
    public int from() {
        return _order._src;
    }
    
    public int to() {
        return _order._dst;
    }
    
    public int ships() {
        return _order._shp;
    }
    
    int turn() {
        return _turn;
    }
    
}
