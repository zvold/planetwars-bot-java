package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import shared.FutureOrder;
import shared.Game;
import shared.Planet;
import shared.Race;
import simulate.ShipsGraphListener;
import simulate.Simulator;
import bot.SimulatorBot;
import bot.algorithms.Pressure;
import bot.algorithms.PressureStatus;


public class PressureTest {

    // distance between planets is 5
    public static final String STATE_1 = "P 14.425990 3.301771 1 100 5\n" +
                                         "P 14.290286 8.040786 2 100 5\n" +
                                         "go\n";
    
    @Test
    public void testPressure1() {
        Game game = new Game(STATE_1);
        SimulatorBot bot = new SimulatorBot() {
        };
        bot._game = game;
        
        Pressure pressure = new Pressure(bot, game.planet(0), Race.ALLY);
        pressure.execute();
        assertTrue("can hold in a simple scenario", pressure.canHold());
        
        pressure = new Pressure(bot, game.planet(0), Race.ALLY);
        pressure.execute(100);
        assertFalse("can't hold when ships are sent", pressure.canHold());
    }

    @Test
    public void testPressure2() {
        Game game = new Game(GameStates.STATE17);
        SimulatorBot bot = new SimulatorBot() {
        };
        bot._game = game;
        
        List<Planet> save = new ArrayList<Planet>();
        for (Planet planet : game.planets())
            save.add(planet.deepCopy());
        
        PressureStatus status = new PressureStatus(bot);
        status.execute();

        for (int i=0; i<game.planets().size(); i++)
            assertTrue("PressureStatus invariant", game.planet(i).deepEquals(save.get(i)));
    }
    
    @Test
    public void testGraphListener() {
        Game game = new Game(STATE_1);
        Simulator sim = new Simulator(game);
        ShipsGraphListener graph = new ShipsGraphListener(Race.ALLY);
        sim.addListener(graph);
        FutureOrder order = new FutureOrder(Race.ALLY, game.planet(0), game.planet(1), 150, 10);
        game.addFutureOrder(order);
        
        sim.simulate(game.planet(0), SimulatorBot.TURNS_PREDICT);
        for (int i=0; i<10; i++)
            assertEquals("can't send anything before 11 turn", 0, graph.shipsAvail(i));
    }
    
    @Test
    public void testPlanetDeepCopy() {
        Game game = new Game(GameStates.STATE3);
        game.addFutureOrder(new FutureOrder(Race.ALLY, game.planet(0), game.planet(1),  151, 11));
        game.addFutureOrder(new FutureOrder(Race.ENEMY, game.planet(1), game.planet(2), 152, 12));
        game.addFutureOrder(new FutureOrder(Race.ALLY, game.planet(2), game.planet(0),  153, 13));
        for (Planet planet : game.planets()) {
            Planet copy = planet.deepCopy();
            assertTrue("deep copy correctness", planet.deepEquals(copy));
        }
    }
}
