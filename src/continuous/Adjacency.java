package continuous;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import shared.Planet;
import shared.Timer;

public class Adjacency extends TimedWork {

    ArrayList<Planet> _planets;
    int _i = 0;
    int _j = 1;
    Map<Planet, ArrayList<Planet>> _adj;
    
    public Adjacency(ArrayList<Planet> planets, Timer timer) {
        super(timer);
        _planets = planets;
        _adj = new HashMap<Planet, ArrayList<Planet>>();
        for (Planet planet : _planets)
            _adj.put(planet, new ArrayList<Planet>());
    }

    @Override
    public void doWorkChunk() {
        Planet p1 = _planets.get(_i);
        _adj.get(p1).addAll(_planets);
        _adj.get(p1).remove(p1);
        
        java.util.Collections.sort(_adj.get(p1), new AdjacencyComparator(p1));
        _i++;
    }

    public ArrayList<Planet> neighbors(Planet planet) {
        return _adj.get(planet);
    }
    
    @Override
    public boolean isDone() {
        return _i >= _planets.size();
    }
    
    @Override
    public String progress() {
        return _i + "/" + _planets.size();
    }
}

class AdjacencyComparator implements Comparator<Planet> {

    Planet _orig;
    
    AdjacencyComparator(Planet orig) {
        _orig = orig;
    }
    
    @Override
    public int compare(Planet p1, Planet p2) {
        return (int)((_orig.distSquared(p1) - _orig.distSquared(p2))); 
    }
    
}