package tests;

import static org.junit.Assert.assertEquals;
import static shared.Race.ALLY;
import static shared.Race.ENEMY;
import static shared.Race.NEUTRAL;

import org.junit.Before;
import org.junit.Test;

import shared.FutureOrder;
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
    
    @Before
    public void setUp() {
        _record = new RecordingOwnerChangeListener();
        _minShips = new MinShipsListener();
        Utils.setLogging(true);
    }
    
    @Test
    public void testSimulator1() {
        Game game = new Game(GameStates.STATE14);
        _simulator = new Simulator(game);
        _simulator.addListener(_record);
        _simulator.addListener(_minShips);
        Planet planet = _simulator.simulate(game.planet(0), 10);
        
        assertEquals("ownership change events", 1, _record.events());
        assertEquals("planet ships", 7, planet.ships());
        assertEquals("events record", "Turn 4: 0:NEUTRAL -> 1:ENEMY\n", _record.record());
        
        assertEquals("neutral min turn", 3, _minShips.turn(NEUTRAL));
        assertEquals("neutral min ships", 0, _minShips.ships(NEUTRAL));
        assertEquals("enemy min turn", 4, _minShips.turn(ENEMY));
        assertEquals("enemy min ships", 1, _minShips.ships(ENEMY));
        assertEquals("allied min turn", -1, _minShips.turn(ALLY));
        assertEquals("allied min ships", 0, _minShips.ships(ALLY));
    }

    @Test
    public void testSimulator2() {
        Game game = new Game(GameStates.STATE15);
        _simulator = new Simulator(game);
        _simulator.addListener(_record);
        _simulator.addListener(_minShips);
        Planet planet = _simulator.simulate(game.planet(0), 10);
        
        assertEquals("ownership change events", 1, _record.events());
        assertEquals("planet ships", 42, planet.ships());
        assertEquals("events record", "Turn 3: 20:NEUTRAL -> 7:ALLY\n", _record.record());

        assertEquals("neutral min turn", 0, _minShips.turn(NEUTRAL));
        assertEquals("neutral min ships", 20, _minShips.ships(NEUTRAL));
        assertEquals("enemy min turn", -1, _minShips.turn(ENEMY));
        assertEquals("enemy min ships", 0, _minShips.ships(ENEMY));
        assertEquals("allied min turn", 3, _minShips.turn(ALLY));
        assertEquals("allied min ships", 7, _minShips.ships(ALLY));
    }

    @Test
    public void testSimulator3() {
        Game game = new Game(GameStates.STATE16);
        _simulator = new Simulator(game);
        _simulator.addListener(_record);
        _simulator.addListener(_minShips);
        Planet planet = _simulator.simulate(game.planet(0), 10);
        
        assertEquals("ownership change events", 2, _record.events());
        assertEquals("planet ships", 3, planet.ships());
        assertEquals("events record", "Turn 4: 1:ALLY -> 1:ENEMY\n" +
                                      "Turn 8: 2:ENEMY -> 1:ALLY\n", _record.record());
        
        assertEquals("neutral min turn", -1, _minShips.turn(NEUTRAL));
        assertEquals("neutral min ships", 0, _minShips.ships(NEUTRAL));
        assertEquals("enemy min turn", 6, _minShips.turn(ENEMY));
        assertEquals("enemy min ships", 0, _minShips.ships(ENEMY));
        assertEquals("allied min turn", 3, _minShips.turn(ALLY));
        assertEquals("allied min ships", 0, _minShips.ships(ALLY));
    }

    @Test
    public void testSimulatorBug() {
        Game game = new Game(GameStates.STATE_BUG);
        _simulator = new Simulator(game);
        _simulator.addListener(_record);
        _simulator.addListener(_minShips);
        Planet planet = _simulator.simulate(game.planet(13), 8);
        assertEquals("owner doesn't change", game.planet(13).owner(), planet.owner());
    }

    @Test
    public void testSimulatorBug2() {
        Game game = new Game(GameStates.STATE_BUG2);
        _simulator = new Simulator(game);
        _simulator.addListener(_record);
        _simulator.addListener(_minShips);
        Planet planet = _simulator.simulate(game.planet(0), 50);
        assertEquals("owner doesn't change", game.planet(0).owner(), planet.owner());
    }

    @Test
    public void testSimulatorBug3() {
        Game game = new Game(GameStates.STATE_BUG3);
        _simulator = new Simulator(game);
        _simulator.addListener(_record);
        _simulator.addListener(_minShips);
        Planet future = _simulator.simulate(game.planet(0), 9);
        assertEquals("check enemy ships", 77, future.ships());
        
        future = _simulator.simulate(game.planet(0), 8);
        assertEquals("check enemy ships", 49, future.ships());
    }
    
    @Test
    public void testSimulator4() {
        Game game = new Game(GameStates.STATE_BUG3);
        _simulator = new Simulator(game);
        _simulator.addListener(_record);
        _simulator.addListener(_minShips);
        Planet future = _simulator.simulate(game.planet(1), 7);
        assertEquals("allied fleet", 1, future.incoming(Race.ALLY).size());
        assertEquals("allied fleet size", 102, future.incoming(Race.ALLY).get(0).ships());
        assertEquals("allied fleet eta", 1, future.incoming(Race.ALLY).get(0).eta());
        
        future = _simulator.simulate(game.planet(1), 9);
        assertEquals("allied fleet", 0, future.incoming(Race.ALLY).size());
    }

    @Test
    public void testSimulatorFuture1() {
        Game game = new Game(GameStates.STATE_FUTURE);
        _simulator = new Simulator(game);
        _simulator.addListener(_record);
        _simulator.addListener(_minShips);
        FutureOrder order = new FutureOrder(Race.ENEMY, game.planet(0), game.planet(1), 1, 4);
        System.out.println(game.planet(0).distance(game.planet(1)));
        game.addFutureOrder(order);
        Planet future0 = _simulator.simulate(game.planet(0), 9);
        assertEquals("planet 0 ships", 5, future0.ships());
        assertEquals("planet 0 owner", Race.ENEMY, future0.owner());
        
        Planet future1 = _simulator.simulate(game.planet(1), 9);
        assertEquals("planet 1 ships", 1, future1.ships());

        future1 = _simulator.simulate(game.planet(1), 20);
        assertEquals("planet 1 ships", 1, future1.ships());
    }

    @Test
    public void testSimulatorFuture2() {
        Game game = new Game(GameStates.STATE_FUTURE2);
        _simulator = new Simulator(game);
        _simulator.addListener(_record);
        _simulator.addListener(_minShips);
        FutureOrder order = new FutureOrder(Race.ENEMY, game.planet(0), game.planet(1), 1, 4);
        System.out.println(game.planet(0).distance(game.planet(1)));
        game.addFutureOrder(order);
        Planet future0 = _simulator.simulate(game.planet(0), 9);
        assertEquals("planet 0 ships", 5, future0.ships());
        assertEquals("planet 0 owner", Race.ALLY, future0.owner());
        
        Planet future1 = _simulator.simulate(game.planet(1), 9);
        assertEquals("planet 1 ships", 1, future1.ships());

        future1 = _simulator.simulate(game.planet(1), 20);
        assertEquals("planet 1 ships", 1, future1.ships());
    }
    
    public void log(String msg) {
        if (Utils._verbose)
            System.err.println(msg);
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
            log(message);
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

        @Override
        public int turn() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean changed() {
            return !"".equals(_record);
        }
    }
    
}
