package tests;

import static org.junit.Assert.assertEquals;
import static shared.Race.ALLY;
import static shared.Race.ENEMY;
import static shared.Race.NEUTRAL;

import org.junit.Test;

import shared.Game;
import shared.Race;

public class GameTest {

    public static final String STATE1 = "P 14.425990 3.301771 0 41 1\n" +
                                        "P 19.466787 20.056168 0 35 5\n" +
                                        "P 3.760394 3.261307 0 35 5\n" +
                                        "F 1 50 1 0 21 20\n" +
                                        "F 2 50 2 1 5 4\n" +
                                        "go\n";

    public static final String STATE2 = "P 14.425990 3.301771 2 41 1\n" +
                                        "P 19.466787 20.056168 1 10 5\n" +
                                        "P 3.760394 3.261307 2 40 5\n" +
                                        "F 1 50 1 1 21 19\n" +
                                        "F 2 50 2 1 5 3\n" +   
                                        "go\n";

    public static final String STATE3 = "P 14.425990 3.301771 0 41 1\n" +
                                        "P 19.466787 20.056168 0 35 5\n" +
                                        "P 3.760394 3.261307 0 35 5\n" +
                                        "F 1 50 3 0 21 19\n" +
                                        "F 2 50 4 0 5 3\n" +
                                        "F 1 50 1 0 21 19\n" +
                                        "F 2 50 2 1 5 3\n" +   
                                        "go\n";

    public static final String STATE4 = "P 14.425990 3.301771 0 41 1\n" +
                                        "P 19.466787 20.056168 0 35 5\n" +
                                        "P 3.760394 3.261307 0 35 5\n" +
                                        "go\n";

    public static final String STATE5 = "P 14.425990 3.301771 0 41 1\n" +
                                        "P 19.466787 20.056168 0 35 5\n" +
                                        "P 3.760394 3.261307 0 35 15\n" +
                                        "go\n";
    
    @Test
    public void testGame() {
        Game game = new Game(STATE1);
        assertEquals("num of planets", 3, game.planets().size());
        assertEquals("num of fleets", 2, game.fleets().size());
        
        assertEquals("incoming ally fleet", 1, game.planet(0).incoming(ALLY).size());
        assertEquals("incoming enemy fleet", 1, game.planet(1).incoming(ENEMY).size());
    }

    @Test
    public void testGameUpdate() {
        Game game = new Game(STATE1);
        game.updateFullState(STATE2);

        assertEquals("planet 0 owner", Race.ENEMY, game.planet(0).owner());
        assertEquals("planet 1 ships", 10, game.planet(1).ships());
        assertEquals("planet 2 growth", 5, game.planet(2).growth());

        assertEquals("incoming ally fleet", 1, game.planet(1).incoming(ALLY).size());
        assertEquals("incoming enemy fleet", 1, game.planet(1).incoming(ENEMY).size());
    }

    @Test
    public void testGameFleetReuse() {
        Game game = new Game(STATE1);
        game.updateFullState(STATE3);

        assertEquals("total fleets", 4, game.fleets().size());
        assertEquals("fleet 0 src", 3, game.fleets().get(0).src());
        assertEquals("fleet 2 src", 1, game.fleets().get(2).src());
    }

    @Test
    public void testGameFleetReuse2() {
        Game game = new Game(STATE1);
        game.updateFullState(STATE3);
        game.updateFullState(STATE2);

        assertEquals("total fleets", 2, game.fleets().size());
        assertEquals("fleet 1 src", 2, game.fleets().get(1).src());
        assertEquals("fleet 1 dst", 1, game.fleets().get(1).dst());
    }

    @Test
    public void testGameUpdateNoFleet() {
        Game game = new Game(STATE1);
        game.updateFullState(STATE3);
        game.updateFullState(STATE4);

        assertEquals("total fleets", 0, game.fleets().size());
        assertEquals("neutral growth", 11, game.growth(NEUTRAL));
        assertEquals("allied growth", 0, game.growth(ALLY));
        assertEquals("enemy growth", 0, game.growth(ENEMY));
    }

    @Test (expected = AssertionError.class)
    public void testGamePlanetInvariant() {
        Game game = new Game(STATE1);
        game.updateFullState(STATE5);
    }
}
