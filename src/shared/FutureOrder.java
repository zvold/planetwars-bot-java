package shared;

public class FutureOrder {

    int    _id;
    Planet _from;
    Planet _to;
    int    _ships;
    int    _turn;   // intial turns at the order's creation time
    Race   _owner;
    
    private static int _counter = 0;
    
    private FutureOrder(int id, Planet from, Planet to, int ships, int turn) {
        _from = from;
        _to = to;
        _ships = ships;
        _turn = turn;
        _id = id;
        _owner = Race.ALLY;
        assert(_ships >= 0) : "ships >= 0";
    }
    
    public FutureOrder(Planet from, Planet to, int ships, int turn) {
        this(_counter++, from, to, ships, turn);
    }
    
    public FutureOrder arrivalCopy() {
        // same id, but is carried out a distance of turns later
        return new FutureOrder(_id, _from, _to, _ships, 
                               _turn + _from.distance(_to));
    }

    public FutureOrder removeArrival() {
        // TODO: can we calculate precise turn beforehand ?
        // yes we can, see below
        FutureOrder found = null;
        int turnFound = -1;
        for (Integer turn : _to._orders.keySet())
            for (FutureOrder order : _to.futureOrders(turn))
                if (order._id == this._id) {
                    found = order;
                    turnFound = turn;
                    break;
                }
        assert(turnFound == _to.distance(_from)) : "check correct turn";
        assert(found != null) : "must have a future order";
        _to._orders.get(turnFound).remove(found);
        return found;
    }
    
    @Override
    public String toString() {
        return "(" + _id + ") " + _ships + ":" + _owner + ", " +  
               _from.id() + " -> " + _to.id() + ", at " + _turn;
    }

    public Race owner() {
        return _owner;
    }
    
    public int id() {
        return _id;
    }
    
    public Planet from() {
        return _from;
    }
    
    public Planet to() {
        return _to;
    }
    
    public int ships() {
        return _ships;
    }
    
    int turn() {
        return _turn;
    }
    
}
