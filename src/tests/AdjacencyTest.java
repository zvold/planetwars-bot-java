package tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import shared.Game;
import shared.Planet;
import shared.Timer;
import shared.Utils;
import continuous.Adjacency;

public class AdjacencyTest {

    public static final String STATE = "P 11.613591 11.658737 0 119 0\n" +
                                       "P 1.290286 9.040786 1 100 5\n" +
                                       "P 21.936895 14.276689 2 100 5\n" +
                                       "P 5.648358 18.265992 0 21 4\n" +
                                       "P 17.578824 5.051482 0 21 4\n" +
                                       "P 0.000000 17.566463 0 32 2\n" +
                                       "P 23.227182 5.751012 0 32 2\n" +
                                       "P 15.996407 22.492537 0 60 5\n" +
                                       "P 7.230774 0.824938 0 60 5\n" +
                                       "P 12.096861 23.317475 0 74 5\n" +
                                       "P 11.130321 0.000000 0 74 5\n" +
                                       "P 5.905729 2.482273 0 85 1\n" +
                                       "P 17.321452 20.835201 0 85 1\n" +
                                       "P 18.286013 0.765778 0 72 3\n" +
                                       "P 4.941168 22.551697 0 72 3\n" +
                                       "P 20.106711 18.059385 0 9 5\n" +
                                       "P 3.120471 5.258090 0 9 5\n" +
                                       "P 4.594839 13.786000 0 69 2\n" +
                                       "P 18.632343 9.531475 0 69 2\n" +
                                       "P 8.801192 20.015703 0 41 1\n" +
                                       "P 14.425990 3.301771 0 41 1\n" +
                                       "P 19.466787 20.056168 0 35 5\n" +
                                       "P 3.760394 3.261307 0 35 5\n" +
                                       "go\n";

    @Test
    public void testAdjacency() {
        Game game = new Game(STATE);
        Timer timer = new Timer();
        timer.start();
        Adjacency adj = new Adjacency(game.planets(), timer);
        Utils.setLogging(false);
        while (!adj.isDone())
            adj.doWork(5);

        for (Planet planet : game.planets()) {
            assertEquals("number of neighbors", game.planets().size() - 1, adj.neighbors(planet).size());
        }

    }

}
