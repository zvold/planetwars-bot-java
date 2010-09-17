package tests;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import shared.Game;
import shared.Race;

public class FleetMergingTest {

    String _data[] = new String[] {GameStates.STATE1, 
                                   GameStates.STATE3, GameStates.STATE4,
                                   GameStates.STATE6, 
                                   GameStates.STATE8,
                                   GameStates.STATE9 };
    Random _rnd = new Random(0);
    
    public void testRandomMergeFleet() {
        Game game = new Game();
        int idx = 0;
        for (int i=0; i<30; i++) {
            idx = _rnd.nextInt(_data.length);
            game.updateFullState(_data[idx]);
        }
        
        Game game2 = new Game(_data[idx]);
        assertEquals("enemy fleets' ships", game2.fleets(Race.ENEMY), game.fleets(Race.ENEMY));
        assertEquals("allied fleets' ships", game2.fleets(Race.ALLY), game.fleets(Race.ALLY));
    }

    @Test
    public void test() {
        for (int i=0; i<100; i++)
            testRandomMergeFleet();
    }
    
}
