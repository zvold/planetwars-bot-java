package tests;

import static org.junit.Assert.assertEquals;
import static shared.Race.ALLY;
import static shared.Race.ENEMY;
import static shared.Race.NEUTRAL;

import org.junit.Before;
import org.junit.Test;

import shared.Game;
import shared.Planet;
import shared.Race;
import shared.Utils;
import simulate.MinShipsListener;
import simulate.OwnerChangeListener;
import simulate.Simulator;


public class SimulatorTest {

    RecordingOwnerChangeListener _record;
    MinShipsListener _minShips;
    Simulator _simulator;
    
    public static final String STATE1 = "P 14.425990 3.301771 0 10 1\n" +
                                        "F 1 10 42 0 10 3\n" +
                                        "F 2 1 42 0 10 4\n" +
                                        "go\n";

    public static final String STATE2 = "P 14.425990 3.301771 0 20 5\n" +
                                        "F 1 30 42 0 10 3\n" +
                                        "F 2 23 42 0 10 3\n" +
                                        "go\n";

    public static final String STATE3 = "P 14.425990 3.301771 1 10 1\n" +
                                        "F 2 13 42 0 10 3\n" +
                                        "F 2 2 42 0 10 4\n" +
                                        "F 1 3 42 0 10 6\n" +
                                        "F 1 3 42 0 10 8\n" +
                                        "go\n";
    
    public static final String STATE_BUG = "P 11.1543457672 11.1230390458 1 13 0\n" +
                                           "P 20.2580532014 17.3378076603 1 3867 5\n" +
                                           "P 2.05063833309 4.90827043135 1 4783 5\n" +
                                           "P 1.73586360053 19.1014098828 1 2844 3\n" +
                                           "P 20.5728279339 3.14466820885 1 2804 3\n" +
                                           "P 4.81222872463 14.3124313485 1 2828 3\n" +
                                           "P 17.4964628098 7.93364674318 1 2821 3\n" +
                                           "P 5.22182783935 21.8247075181 1 1912 2\n" +
                                           "P 17.0868636951 0.421370573528 1 1879 2\n" +
                                           "P 15.9064827434 10.0049143921 1 4695 5\n" +
                                           "P 6.40220879104 12.2411636996 1 4754 5\n" +
                                           "P 8.03902742352 0.0 1 923 1\n" +
                                           "P 14.2696641109 22.2460780917 1 957 1\n" +
                                           "P 16.8754841957 20.3188307189 2 5 2\n" +
                                           "P 5.43320733871 1.92724737271 1 1850 2\n" +
                                           "P 17.2186646717 18.7889628728 1 3120 4\n" +
                                           "P 5.09002686278 3.45711521886 1 3789 4\n" +
                                           "P 4.67339275292 0.0733048941562 1 1849 2\n" +
                                           "P 17.6352987815 22.1727731975 1 1875 2\n" +
                                           "P 10.1881521031 14.0031909972 1 2864 3\n" +
                                           "P 12.1205394314 8.24288709442 1 2827 3\n" +
                                           "P 22.3086915345 21.9078005574 1 2661 3\n" +
                                           "P 0.0 0.338277534212 1 2779 3\n" +
                                           "F 1 1 1 13 5 1\n" +
                                           "F 1 1 1 13 5 2\n" +
                                           "F 1 1 1 13 5 3\n" +
                                           "F 1 1 21 13 6 4\n" +
                                           "F 1 1 1 13 5 4\n" +
                                           "F 1 1 15 13 2 1\n" +
                                           "go\n";
    
    @Before
    public void setUp() {
        _record = new RecordingOwnerChangeListener();
        _minShips = new MinShipsListener();
        _simulator = new Simulator();
        _simulator.addListener(_record);
        _simulator.addListener(_minShips);
        Utils.setLogging(true);
    }
    
    @Test
    public void testSimulator1() {
        Game game = new Game(STATE1);
        Planet planet = _simulator.simulate(game.planet(0), 10);
        
        assertEquals("ownership change events", 1, _record.events());
        assertEquals("planet ships", 7, planet.ships());
        assertEquals("events record", "Turn 4: 0:NEUTRAL -> 1:ENEMY\n", _record.record());
        
        assertEquals("neutral min turn", 3, _minShips.turn(NEUTRAL));
        assertEquals("neutral min ships", 0, _minShips.ships(NEUTRAL));
        assertEquals("enemy min turn", 4, _minShips.turn(ENEMY));
        assertEquals("enemy min ships", 1, _minShips.ships(ENEMY));
        assertEquals("allied min turn", -1, _minShips.turn(ALLY));
        assertEquals("allied min ships", Integer.MAX_VALUE, _minShips.ships(ALLY));
    }

    @Test
    public void testSimulator2() {
        Game game = new Game(STATE2);
        Planet planet = _simulator.simulate(game.planet(0), 10);
        
        assertEquals("ownership change events", 1, _record.events());
        assertEquals("planet ships", 42, planet.ships());
        assertEquals("events record", "Turn 3: 20:NEUTRAL -> 7:ALLY\n", _record.record());

        assertEquals("neutral min turn", 0, _minShips.turn(NEUTRAL));
        assertEquals("neutral min ships", 20, _minShips.ships(NEUTRAL));
        assertEquals("enemy min turn", -1, _minShips.turn(ENEMY));
        assertEquals("enemy min ships", Integer.MAX_VALUE, _minShips.ships(ENEMY));
        assertEquals("allied min turn", 3, _minShips.turn(ALLY));
        assertEquals("allied min ships", 7, _minShips.ships(ALLY));
    }

    @Test
    public void testSimulator3() {
        Game game = new Game(STATE3);
        Planet planet = _simulator.simulate(game.planet(0), 10);
        
        assertEquals("ownership change events", 2, _record.events());
        assertEquals("planet ships", 3, planet.ships());
        assertEquals("events record", "Turn 4: 1:ALLY -> 1:ENEMY\n" +
                                      "Turn 8: 2:ENEMY -> 1:ALLY\n", _record.record());
        
        assertEquals("neutral min turn", -1, _minShips.turn(NEUTRAL));
        assertEquals("neutral min ships", Integer.MAX_VALUE, _minShips.ships(NEUTRAL));
        assertEquals("enemy min turn", 6, _minShips.turn(ENEMY));
        assertEquals("enemy min ships", 0, _minShips.ships(ENEMY));
        assertEquals("allied min turn", 3, _minShips.turn(ALLY));
        assertEquals("allied min ships", 0, _minShips.ships(ALLY));
    }

    @Test
    public void testSimulatorBug() {
        Game game = new Game(STATE_BUG);
        Planet planet = _simulator.simulate(game.planet(13), 8);
        assertEquals("owner doesn't change", game.planet(13).owner(), planet.owner());
    }
    
    class RecordingOwnerChangeListener implements OwnerChangeListener {

        int _numEvents;
        String _record = "";
        
        @Override
        public void ownerChanged(int turn, 
                                 Race fromRace, int fromShips,
                                 Race toRace, int toShips) {
            String message = "Turn " + turn + ": " + 
                             fromShips + ":" + fromRace + " -> " + 
                             toShips + ":" + toRace; 
            Utils.log(message);
            _record += message + "\n";
            
            _numEvents++;
        }
        
        public int events() {
            return _numEvents;
        }
        
        public String record() {
            return _record; 
        }

        @Override
        public void reset() {
            _numEvents = 0;
            _record = "";
        }
    }
    
}
