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

    public static final String STATE1 = "P 14.425990 3.301771 0 41 1\n" +
                                        "P 19.466787 20.056168 0 35 5\n" +
                                        "P 3.760394 3.261307 0 35 8\n" +
                                        "go\n";

    public static final String STATE2 = "P 14.425990 3.301771 2 41 1\n" +
                                        "P 19.466787 20.056168 1 10 5\n" +
                                        "P 3.760394 3.261307 2 40 8\n" +
                                        "go\n";

    public static final String STATE3 = "P 14.425990 3.301771 1 41 1\n" +
                                        "P 19.466787 20.056168 2 35 5\n" +
                                        "P 3.760394 3.261307 0 35 8\n" +
                                        "go\n";

    public static final String STATE4 = "P 14.425990 3.301771 2 41 1\n" +
                                        "P 19.466787 20.056168 1 35 5\n" +
                                        "P 3.760394 3.261307 0 35 8\n" +
                                        "go\n";

    private void updateByOneLine(Game game, String state) {
        String[] lines = state.split("\n");
        for (String line : lines)
            game.updateOneLine(line);
        game.resetTurn();
    }
    
    @Test
    public void testGameOwnerTrack() {
        Game game = new Game(STATE1);
        assertEquals("num of neutral planets", 3, game.planets(NEUTRAL).size());
        assertEquals("num of allied planets", 0, game.planets(ALLY).size());
        assertEquals("num of enemy planets", 0, game.planets(ENEMY).size());
    }

    @Test
    public void testGameOwnerTrack2() {
        Game game = new Game(STATE1);
        game.resetTurn();
        updateByOneLine(game, STATE2);

        assertEquals("num of neutral planets", 0, game.planets(NEUTRAL).size());
        assertEquals("num of allied planets", 1, game.planets(ALLY).size());
        assertEquals("num of enemy planets", 2, game.planets(ENEMY).size());
        assertEquals("allied planet id", 1, new ArrayList<Planet>(game.planets(ALLY)).get(0).id());
    }

    @Test
    public void testGameOwnerTrack3() {
        Game game = new Game(STATE1);
        game.updateFullState(STATE3);

        assertEquals("num of neutral planets", 1, game.planets(NEUTRAL).size());
        assertEquals("num of allied planets", 1, game.planets(ALLY).size());
        assertEquals("num of enemy planets", 1, game.planets(ENEMY).size());
        assertEquals("neutral planet id", 2, new ArrayList<Planet>(game.planets(NEUTRAL)).get(0).id());
    }

    @Test
    public void testGameOwnerTrack4() {
        Game game = new Game(STATE1);
        game.updateFullState(STATE3);
        
        assertEquals("neutral growth", 8, game.growth(NEUTRAL));
        assertEquals("allied growth", 1, game.growth(ALLY));
        assertEquals("enemy growth", 5, game.growth(ENEMY));
        
        game.updateFullState(STATE2);

        assertEquals("num of neutral planets", 0, game.planets(NEUTRAL).size());
        assertEquals("num of allied planets", 1, game.planets(ALLY).size());
        assertEquals("num of enemy planets", 2, game.planets(ENEMY).size());
        assertEquals("allied planet id", 1, new ArrayList<Planet>(game.planets(ALLY)).get(0).id());
    }

    @Test
    public void testGameOwnerTrack5() {
        Game game = new Game(STATE1);
        game.updateFullState(STATE3);
        game.resetTurn();
        updateByOneLine(game, STATE4);

        assertEquals("num of neutral planets", 1, game.planets(NEUTRAL).size());
        assertEquals("num of allied planets", 1, game.planets(ALLY).size());
        assertEquals("num of enemy planets", 1, game.planets(ENEMY).size());
        assertEquals("allied planet id", 1, new ArrayList<Planet>(game.planets(ALLY)).get(0).id());
    }
    
}
