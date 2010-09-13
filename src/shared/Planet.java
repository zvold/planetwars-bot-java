package shared;

import java.util.regex.Matcher;

public class Planet {

    int     _id;
    double  _x;
    double  _y;
    int     _ships;
    int     _growth;
    Race    _owner;
    
    public Planet(int id, Matcher m) {
        assert(m.groupCount() == 5) : "check planet pattern group count";
        _id = id;
        _x = Double.parseDouble(m.group(1));
        _y = Double.parseDouble(m.group(2));
        _owner = Race.values()[Integer.parseInt(m.group(3))];
        _ships = Integer.parseInt(m.group(4));
        _growth = Integer.parseInt(m.group(5));
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Planet " + _id);
        b.append(" (" + _ships + ":" + _owner);
        b.append(" +" + _growth + ")");
        return b.toString();
    }
    
}
