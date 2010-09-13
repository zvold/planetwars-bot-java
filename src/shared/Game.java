package shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Game {

    boolean     _isFirstTurn = true;
    Map<Integer, Planet> _planets;
    List<Fleet> _fleets;
    
    public Game(String from) {
        initialize(from);
    }
    
    public void update(String from) {
        if (_isFirstTurn) {
            initialize(from);
        } else {
        }
    }
    
    public void initialize(String from) {
        _planets = new HashMap<Integer, Planet>();
        _fleets = new ArrayList<Fleet>();
        
        Parser parser = new Parser(from);
        
        while (parser.hasNext()) {
            Object next = parser.next();
            switch (parser.kind()) {
                case FLEET:
                    _fleets.add((Fleet)next);
                    break;
                case PLANET: {
                    Planet planet = (Planet)next; 
                    _planets.put(planet._id, planet);
                    break;
                }
                default:
                    break;
            }
        }
        
        _isFirstTurn = false;
    }
    
    public Collection<Planet> planets() {
        return _planets.values();
    }
    
    public List<Fleet> fleets() {
        return _fleets;
    }
    
}
