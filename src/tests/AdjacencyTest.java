package tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import shared.Game;
import shared.Planet;
import shared.Timer;
import shared.Utils;
import bot.BaseBot;
import continuous.Adjacency;

public class AdjacencyTest {

    @Test
    public void testAdjacency() {
        Game game = new Game(GameStates.STATE17);
        Timer timer = new Timer();
        timer.start();
        Adjacency adj = new Adjacency(game.planets(), timer, 
                                      new BaseBot() {
                                        @Override
                                        public void doTurn() {
                                        }
                                      });
        Utils.setLogging(false);
        while (!adj.isDone())
            adj.doWork(5);

        for (Planet planet : game.planets()) {
            assertEquals("number of neighbors", game.planets().size() - 1, adj.neighbors(planet).size());
        }

    }

}
