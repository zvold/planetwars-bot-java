package filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import shared.Planet;

public abstract class PlanetFilter implements Iterable<Planet> {

    Collection<Planet> _planets;
    
    public PlanetFilter(Collection<Planet> planets) {
        _planets = planets;
    }
    
    @Override
    public Iterator<Planet> iterator() {
        return new PlanetIterator();
    }

    public List<Planet> select() {
        List<Planet> ret = new ArrayList<Planet>();
        for (Planet planet : this)
            ret.add(planet);
        return ret;
    }
    
    public abstract boolean filter(Planet planet);
    
    class PlanetIterator implements Iterator<Planet> {

        Iterator<Planet> _iter;
        Planet           _next;
        
        PlanetIterator() {
            _iter = _planets.iterator();
            searchForNext();
        }

        @Override
        public boolean hasNext() {
            return _next != null;
        }

        @Override
        public Planet next() {
            Planet ret = _next;
            searchForNext();
            assert(ret != null) : "user should've checked with hasNext()";
            return ret;
        }
        
        void searchForNext() {
            _next = null;
            while (_iter.hasNext()) {
                Planet planet = _iter.next();
                if (filter(planet)) {
                    _next = planet;
                    break;
                }
            }
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }
    
}
