package compare;

import java.util.ArrayList;
import java.util.Comparator;

import shared.Planet;

class AddComparator<K extends Planet, E> implements Comparator<Pair<K, E>>, IScore<K> {

    ArrayList<IScore<K>> _scorers = new ArrayList<IScore<K>>();
    
    public AddComparator(IScore<K>... scorers) {
        for (IScore<K> scorer : scorers)
            _scorers.add(scorer);
    }
    
    @Override
    public int compare(Pair<K, E> p1, Pair<K, E> p2) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public double score(K target) {
        double ret = 0;
        for (IScore<K> scorer : _scorers)
            ret += scorer.score(target);
        return ret;
    }
    
}
