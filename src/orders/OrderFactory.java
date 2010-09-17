package orders;

import java.util.WeakHashMap;

public class OrderFactory {

    static WeakHashMap<Integer, Order> _cache = new WeakHashMap<Integer, Order>();
    
    public static Order createOrder(int src, int dst, int ships) {
        Order ret = _cache.get(Order.hash(src, dst, ships));
        if (ret == null) {
            ret = new Order(src, dst, ships);
            _cache.put(ret.hashCode(), ret);
        }
        assert(ret != null) : "can't return null";
        return ret;
    }
    
}
