package compare;

import shared.Planet;

public class AdjacencyComparator implements IScore<Planet> {

    Planet _orig;
    
    public AdjacencyComparator(Planet orig) {
        _orig = orig;
    }
    
    @Override
    public int compare(Planet p1, Planet p2) {
        return (int)(100.0d * (score(p1) - score(p2))); 
    }

    @Override
    public double score(Planet p) {
        return _orig.distSquared(p);
    }
    
}
