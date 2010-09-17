package tests;

import static org.junit.Assert.assertEquals;
import static shared.Race.ALLY;
import static shared.Race.ENEMY;
import static shared.Race.NEUTRAL;

import java.util.ArrayList;

import org.junit.Test;

import shared.Game;
import shared.Planet;

public class GameTestTrack {

    private void updateByOneLine(Game game, String state) {
        String[] lines = state.split("\n");
        for (String line : lines)
            game.updateOneLine(line);
        game.resetTurn();
    }
    
    @Test
    public void testGameOwnerTrack() {
        Game game = new Game(GameStates.STATE10);
        assertEquals("num of neutral planets", 3, game.planets(NEUTRAL).size());
        assertEquals("num of allied planets", 0, game.planets(ALLY).size());
        assertEquals("num of enemy planets", 0, game.planets(ENEMY).size());
    }

    @Test
    public void testGameOwnerTrack2() {
        Game game = new Game(GameStates.STATE10);
        game.resetTurn();
        updateByOneLine(game, GameStates.STATE11);

        assertEquals("num of neutral planets", 0, game.planets(NEUTRAL).size());
        assertEquals("num of allied planets", 1, game.planets(ALLY).size());
        assertEquals("num of enemy planets", 2, game.planets(ENEMY).size());
        assertEquals("allied planet id", 1, new ArrayList<Planet>(game.planets(ALLY)).get(0).id());
    }

    @Test
    public void testGameOwnerTrack3() {
        Game game = new Game(GameStates.STATE10);
        game.updateFullState(GameStates.STATE12);

        assertEquals("num of neutral planets", 1, game.planets(NEUTRAL).size());
        assertEquals("num of allied planets", 1, game.planets(ALLY).size());
        assertEquals("num of enemy planets", 1, game.planets(ENEMY).size());
        assertEquals("neutral planet id", 2, new ArrayList<Planet>(game.planets(NEUTRAL)).get(0).id());
    }

    @Test
    public void testGameOwnerTrack4() {
        Game game = new Game(GameStates.STATE10);
        game.updateFullState(GameStates.STATE12);
        
        assertEquals("neutral growth", 8, game.growth(NEUTRAL));
        assertEquals("allied growth", 1, game.growth(ALLY));
        assertEquals("enemy growth", 5, game.growth(ENEMY));
        
        game.updateFullState(GameStates.STATE11);

        assertEquals("num of neutral planets", 0, game.planets(NEUTRAL).size());
        assertEquals("num of allied planets", 1, game.planets(ALLY).size());
        assertEquals("num of enemy planets", 2, game.planets(ENEMY).size());
        assertEquals("allied planet id", 1, new ArrayList<Planet>(game.planets(ALLY)).get(0).id());
    }

    @Test
    public void testGameOwnerTrack5() {
        Game game = new Game(GameStates.STATE10);
        game.updateFullState(GameStates.STATE12);
        game.resetTurn();
        updateByOneLine(game, GameStates.STATE13);

        assertEquals("num of neutral planets", 1, game.planets(NEUTRAL).size());
        assertEquals("num of allied planets", 1, game.planets(ALLY).size());
        assertEquals("num of enemy planets", 1, game.planets(ENEMY).size());
        assertEquals("allied planet id", 1, new ArrayList<Planet>(game.planets(ALLY)).get(0).id());
    }
    
}
