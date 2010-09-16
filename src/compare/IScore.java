package compare;

import java.util.Comparator;

public interface IScore<K> extends Comparator<K> {

    public double score(K p);
    
}
