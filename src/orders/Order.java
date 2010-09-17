package orders;

public class Order {

    public final int     _src;
    public final int     _dst;
    public final int     _shp;
    
    Order(int src, int dst, int ships) {
        _src = src;
        _dst = dst;
        _shp = ships;
    }

    @Override
    public int hashCode() {
        return Order.hash(_src, _dst, _shp);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Order) {
            Order that = (Order)obj;
            return _src == that._src &&
                   _dst == that._dst &&
                   _shp == that._shp;
        }
        return false;
    }
    
    public static int hash(int src, int dst, int ships) {
        return (src << 0) +
               (dst << 8) +
               (ships << 16);
    }
    
}
