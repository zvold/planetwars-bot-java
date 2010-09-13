package compare;

public class Pair<K, V> {
    
    public K _key;
    public V _val;
    
    public Pair (K k, V v) {
        _key = k;
        _val = v;
    }
    
    @Override
    public int hashCode() {
        return _key.hashCode();
    }
    
    @Override
    public boolean equals(Object that) {
        if (that instanceof Pair) {
            return ((Pair<?, ?>)that)._key.equals(_key);
        }
        return false;
    }

    @Override
    public String toString() {
        return _key + " - " + _val;
    }
}
