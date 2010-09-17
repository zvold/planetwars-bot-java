package filters;

import java.util.ArrayList;

import shared.Planet;

public class SortedDistanceListHead extends PlanetFilter {

    Planet  _to;
    int     _than;
    boolean _finished;
    
    public SortedDistanceListHead(ArrayList<Planet> planets, Planet to, int than) {
        super(planets);
        _to = to;
        _than = than;
        assert(checkSortedness()) : "sorted array expected";
    }

    @Override
    public boolean filter(Planet planet) {
        boolean ret = planet.distance(_to) <= _than;
        if (!ret)
            _finished = true;
        return ret;
    }

    private boolean checkSortedness() {
        ArrayList<Planet> list = new ArrayList<Planet>(_planets);
        for (int i=0; i<list.size()-1; i++) {
            Planet p0 = list.get(i);
            Planet p1 = list.get(i+1);
            if (p1.distance(_to) < p0.distance(_to))
                return false;
        }
        return true;
    }

    class FilterCloserToThanIterator extends PlanetIterator {
        
        @Override
        public boolean hasNext() {
            return _finished ? false : super.hasNext();
        }
        
    }
    
}
