package shared;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    public static enum Kind { PLANET, FLEET, NONE }

    //                                                     P    x        y     owner  ships  growth
    static final Pattern _planetPattern = Pattern.compile("P ([^ #]+) ([^ #]+) (\\d+) (\\d+) (\\d+)");

    //                                                    F owner  ships   src     dst  turns  turnsRem
    static final Pattern _fleetPattern = Pattern.compile("F (\\d+) (\\d+) (\\d+) (\\d+) (\\d+) (\\d+)");
    
    static String  _state;
    static int     _start;
    static int     _end;
    static Matcher _planetMatcher;
    static Matcher _fleetMatcher;
    static Kind    _kind = Kind.NONE;
    static int     _planetId;
    static int     _fleetId;
    
    private Parser() { 
    }

    public static boolean hasNext() {
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
    
    private static void shiftMatchers() {
        _start = (_end == _state.length()) ? _end : _end + 1;
        _end = _state.indexOf('\n', _start);
        _end = _end == -1 ? _state.length() : _end;
        
        _planetMatcher.region(_start, _end);
        _fleetMatcher.region(_start, _end);
    }
    
    public static Object next() {
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

    public static Kind kind() {
        return _kind;
    }

    public static Planet updatePlanet(ArrayList<Planet> _planets) {
        Planet planet;
        if (_planetId < _planets.size()) {
            planet = _planets.get(_planetId);
            assert(planet != null) : "must have this planet";
            planet.update(_planetMatcher);
        } else {
            planet = new Planet(_planetId, _planetMatcher);
            _planets.add(planet);
        }
        _planetId++;
        shiftMatchers();
        return planet;
    }

    public static void setSingleLine(String line) {
        _state = line;
        _start = 0;
        _end = line.length();
        _planetMatcher = _planetPattern.matcher(line);
        _fleetMatcher = _fleetPattern.matcher(line);
    }
    
    public static void reset() {
        _planetId = 0;
        _fleetId = 0;
    }
    
    public static void discard() {
        shiftMatchers();
    }

    public static void init(String from) {
        reset();
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
    
}

