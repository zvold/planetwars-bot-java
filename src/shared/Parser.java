package shared;

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
    
    Parser(String from) { 
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

        _start = (_end == _state.length()) ? _end : _end + 1;
        _end = _state.indexOf('\n', _start);
        _end = _end == -1 ? _state.length() : _end;
        
        _planetMatcher.region(_start, _end);
        _fleetMatcher.region(_start, _end);
        
        return ret;
    }

    public Kind kind() {
        return _kind;
    }
    
}

