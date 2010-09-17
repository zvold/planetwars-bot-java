package continuous;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import shared.Planet;
import shared.Race;
import shared.Timer;
import utils.ILogger;

import compare.AdjacencyComparator;

public class Adjacency extends TimedWork {

    ArrayList<Planet> _planets;
    int _i = 0;
    int _j = 1;
    Map<Planet, ArrayList<Planet>> _adj;
    
    public Adjacency(ArrayList<Planet> planets, Timer timer, ILogger logger) {
        super(timer, logger);
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
        
        Collections.sort(_adj.get(p1), new AdjacencyComparator(p1));
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

    public Planet getNearestNeighbor(Planet planet, Race owner) {
        for (Planet ret : _adj.get(planet))
            if (ret.owner() == owner)
                return ret;
        return null;
    }
    
}
