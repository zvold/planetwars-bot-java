package compare;

import java.util.ArrayList;

import shared.Planet;

class AddComparator<K extends Planet, E> implements IScore<Pair<K, E>> {

    ArrayList<IScore<Pair<K, E>>> _scorers = new ArrayList<IScore<Pair<K, E>>>();
    
    public AddComparator(IScore<Pair<K, E>>... scorers) {
        for (IScore<Pair<K, E>> scorer : scorers)
            _scorers.add(scorer);
    }
    
    @Override
    public int compare(Pair<K, E> p1, Pair<K, E> p2) {
        throw new RuntimeException("not implemented yet");
    }
    
    @Override
    public double score(Pair<K, E> pair) {
        double ret = 0;
        for (IScore<Pair<K, E>> scorer : _scorers)
            ret += scorer.score(pair);
        return ret;
    }
    
}
