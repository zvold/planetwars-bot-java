package tests;

import static org.junit.Assert.assertEquals;
import static shared.Race.ALLY;
import static shared.Race.ENEMY;
import static shared.Race.NEUTRAL;

import org.junit.Test;

import shared.Game;
import shared.Race;

public class GameTest {

    @Test
    public void testGame() {
        Game game = new Game(GameStates.STATE1);
        assertEquals("num of planets", 3, game.planets().size());
        assertEquals("num of fleets", 2, game.fleets().size());
        
        assertEquals("incoming ally fleet", 1, game.planet(0).incoming(ALLY).size());
        assertEquals("incoming enemy fleet", 1, game.planet(1).incoming(ENEMY).size());
    }

    @Test
    public void testGameUpdate() {
        Game game = new Game(GameStates.STATE1);
        game.updateFullState(GameStates.STATE2);

        assertEquals("planet 0 owner", Race.ENEMY, game.planet(0).owner());
        assertEquals("planet 1 ships", 10, game.planet(1).ships());
        assertEquals("planet 2 growth", 5, game.planet(2).growth());

        assertEquals("incoming ally fleet", 1, game.planet(1).incoming(ALLY).size());
        assertEquals("incoming enemy fleet", 1, game.planet(1).incoming(ENEMY).size());
    }

    @Test
    public void testGameFleetReuse() {
        Game game = new Game(GameStates.STATE1);
        game.updateFullState(GameStates.STATE3);

        assertEquals("total fleets", 4, game.fleets().size());
        assertEquals("fleet 0 src", 3, game.fleets().get(0).src());
        assertEquals("fleet 2 src", 1, game.fleets().get(2).src());
    }

    @Test
    public void testGameFleetReuse2() {
        Game game = new Game(GameStates.STATE1);
        game.updateFullState(GameStates.STATE3);
        game.updateFullState(GameStates.STATE2);

        assertEquals("total fleets", 2, game.fleets().size());
        assertEquals("fleet 1 src", 2, game.fleets().get(1).src());
        assertEquals("fleet 1 dst", 1, game.fleets().get(1).dst());
    }

    @Test
    public void testGameUpdateNoFleet() {
        Game game = new Game(GameStates.STATE1);
        game.updateFullState(GameStates.STATE3);
        game.updateFullState(GameStates.STATE4);

        assertEquals("total fleets", 0, game.fleets().size());
        assertEquals("neutral growth", 11, game.growth(NEUTRAL));
        assertEquals("allied growth", 0, game.growth(ALLY));
        assertEquals("enemy growth", 0, game.growth(ENEMY));
    }

    @Test (expected = AssertionError.class)
    public void testGamePlanetInvariant() {
        Game game = new Game(GameStates.STATE1);
        game.updateFullState(GameStates.STATE5);
    }
}
