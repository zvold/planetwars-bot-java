package tests;

import static org.junit.Assert.*;

import org.junit.Test;

import shared.Game;

public class GameTest {

    @Test
    public void testGame() {
        Game game = new Game("P 14.425990 3.301771 0 41 1\n" +                                                                                                                                  
                             "P 19.466787 20.056168 0 35 5\n" +                                                                                                                                 
                             "P 3.760394 3.261307 0 35 5\n" +                                                                                                                                   
                             "F 1 50 1 15 21 20\n" +                                                                                                                                            
                             "F 2 50 2 15 5 4\n" +
                             "go\n");
        assertEquals("num of planets", 3, game.planets().size());
        assertEquals("num of fleets", 2, game.fleets().size());
    }
    
}
