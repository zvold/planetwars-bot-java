package shared;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    public enum Kind { PLANET, FLEET, NONE }

    //                                                     P    x        y     owner  ships  growth
    static final Pattern _planetPattern = Pattern.compile("P ([^ #]+) ([^ #]+) (\\d+) (\\d+) (\\d+)");

    //                                                    F owner  ships   src     dst  turns  turnsRem
    static final Pattern _fleetPattern = Pattern.compile("F (\\d+) (\\d+) (\\d+) (\\d+) (\\d+) (\\d+)");
    
    String  _state;
    int     _start;
    int     _end;
    Matcher _planetMatcher;
    Matcher _fleetMatcher;
    Kind    _kind = Kind.NONE;
    int     _planetId;
    int     _fleetId;
    
    public Parser(String from) { 
        _state = from;
        _start = 0;
        _end = _state.indexOf('\n');
        if (_end == -1)
            _end = _state.length();
        
        _planetMatcher = _planetPattern.matcher(_state);
        _planetMatcher.region(_start, _end);
        
        _fleetMatcher = _fleetPattern.matcher(_state);
        _fleetMatcher.region(_start, _end);
    }

    public boolean hasNext() {
        if (_start >= _state.length())
            return false;

        _kind = Kind.NONE;
        
        if (_fleetMatcher.lookingAt()) {
            assert(!_planetMatcher.lookingAt()) : "simultaneous fleet & planet match";            
            _kind = Kind.FLEET;
        }
        
        if (_planetMatcher.lookingAt()) {
            assert(!_fleetMatcher.lookingAt()) : "simultaneous fleet & planet match";
            _kind = Kind.PLANET;
        }
        
        return true;
    }
    
    private void shiftMatchers() {
        _start = (_end == _state.length()) ? _end : _end + 1;
        _end = _state.indexOf('\n', _start);
        _end = _end == -1 ? _state.length() : _end;
        
        _planetMatcher.region(_start, _end);
        _fleetMatcher.region(_start, _end);
    }
    
    public Object next() {
        Object ret = null;
        switch (_kind) {
            case FLEET:
                ret = new Fleet(_fleetId++, _fleetMatcher);
                break;
            case PLANET:
                ret = new Planet(_planetId++, _planetMatcher);
                break;
            default:
                break;
        }
        shiftMatchers();
        return ret;
    }

    public Kind kind() {
        return _kind;
    }

    public Planet updatePlanet(ArrayList<Planet> _planets) {
        Planet planet;
        if (_planetId < _planets.size()) {
            planet = _planets.get(_planetId);
            assert(planet != null) : "must have this fleet";
            planet.update(_planetMatcher);
        } else {
            planet = new Planet(_planetId, _planetMatcher);
            _planets.add(planet);
        }
        _planetId++;
        shiftMatchers();
        return planet;
    }

    public Fleet updateFleet(ArrayList<Fleet> _fleets) {
        Fleet fleet;
        if (_fleetId < _fleets.size()) {
            fleet = _fleets.get(_fleetId);
            assert(fleet != null) : "must have this fleet";
            fleet.update(_fleetMatcher);
        } else {
            fleet = new Fleet(_fleetId, _fleetMatcher);
            _fleets.add(fleet);
        }
        _fleetId++;
        shiftMatchers();
        return fleet;
    }
    
    public void setSingleLine(String line) {
        _state = line;
        _start = 0;
        _end = line.length();
        _planetMatcher = _planetPattern.matcher(line);
        _fleetMatcher = _fleetPattern.matcher(line);
    }
    
    public void reset() {
        _planetId = 0;
        _fleetId = 0;
    }
    
    public void discard() {
        shiftMatchers();
    }
    
}

