package tests;

import static org.junit.Assert.assertEquals;
import static shared.Race.ALLY;
import static shared.Race.ENEMY;

import org.junit.Test;

import shared.Game;
import shared.Race;

public class GameTest2 {

    public static final String STATE1 = "P 14.425990 3.301771 0 41 1\n" +
                                        "P 19.466787 20.056168 0 35 5\n" +
                                        "P 3.760394 3.261307 0 35 5\n" +
                                        "F 1 50 1 15 21 20\n" +
                                        "F 2 50 2 15 5 4\n" +
                                        "go\n";

    public static final String STATE2 = "P 14.425990 3.301771 2 41 1\n" +
                                        "P 19.466787 20.056168 1 10 5\n" +
                                        "P 3.760394 3.261307 2 40 5\n" +
                                        "F 1 50 1 15 21 19\n" +
                                        "F 2 50 2 15 5 3\n" +
                                        "go\n";

    public static final String STATE3 = "P 14.425990 3.301771 0 41 1\n" +
                                        "P 19.466787 20.056168 0 35 5\n" +
                                        "P 3.760394 3.261307 0 35 5\n" +
                                        "F 1 50 3 0 1 19\n" +
                                        "F 2 50 4 0 1 3\n" +
                                        "F 1 50 1 0 1 19\n" +
                                        "F 2 50 2 1 1 3\n" +   
                                        "go\n";

    public static final String STATE4 = "P 14.425990 3.301771 0 41 1\n" +
                                        "P 19.466787 20.056168 0 35 5\n" +
                                        "P 3.760394 3.261307 0 35 5\n" +
                                        "go\n";
    
    private void updateByOneLine(Game game, String state) {
        String[] lines = state.split("\n");
        for (String line : lines)
            game.updateOneLine(line);
        game.resetTurn();
    }

    @Test
    public void testGameOneLine() {
        Game game = new Game();
        updateByOneLine(game, STATE1);
        assertEquals("num of planets", 3, game.planets().size());
        assertEquals("num of fleets", 2, game.fleets().size());
    }

    @Test
    public void testGameUpdate() {
        Game game = new Game();
        updateByOneLine(game, STATE1);
        updateByOneLine(game, STATE2);

        assertEquals("planet 0 owner", Race.ENEMY, game.planet(0).owner());
        assertEquals("planet 1 ships", 10, game.planet(1).ships());
        assertEquals("planet 2 growth", 5, game.planet(2).growth());
    }

    @Test
    public void testGameFleetReuse() {
        Game game = new Game();
        updateByOneLine(game, STATE1);
        updateByOneLine(game, STATE3);

        assertEquals("total fleets", 4, game.fleets().size());
        assertEquals("fleet 0 src", 3, game.fleets().get(0).src());
        assertEquals("fleet 2 src", 1, game.fleets().get(2).src());
        
        assertEquals("incoming ally fleet", 2, game.planet(0).incoming(ALLY).size());
        assertEquals("incoming enemy fleet", 1, game.planet(0).incoming(ENEMY).size());
        assertEquals("incoming ally fleet", 0, game.planet(1).incoming(ALLY).size());
        assertEquals("incoming enemy fleet", 1, game.planet(1).incoming(ENEMY).size());
    }

    @Test
    public void testGameFleetReuse2() {
        Game game = new Game();
        updateByOneLine(game, STATE1);
        updateByOneLine(game, STATE3);
        updateByOneLine(game, STATE2);

        assertEquals("total fleets", 2, game.fleets().size());
        assertEquals("fleet 1 src", 2, game.fleets().get(1).src());
        assertEquals("fleet 1 dst", 15, game.fleets().get(1).dst());
    }

    @Test
    public void testGameUpdateNoFleet() {
        Game game = new Game();
        updateByOneLine(game, STATE1);
        updateByOneLine(game, STATE3);
        updateByOneLine(game, STATE4);

        assertEquals("total fleets", 0, game.fleets().size());
    }
    
}
