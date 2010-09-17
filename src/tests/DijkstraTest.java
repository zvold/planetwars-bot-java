package tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import shared.Game;
import shared.Planet;
import shared.Race;
import shared.Timer;
import bot.BaseBot;
import continuous.Dijkstra;


public class DijkstraTest {

    public static String STATE = "P 11.0357346548 9.73268211024 1 91 0\n" + // 0
                                 "P 14.543106046 18.2382422455 0 100 5\n" +
                                 "P 7.52836326358 1.22712197494 1 100 5\n" +// 2
                                 "P 8.84824562115 12.2097448727 1 29 5\n" + // 3
                                 "P 13.2232236885 7.25561934776 0 29 5\n" +
                                 "P 0.371764263536 2.27402333576 1 81 1\n" +// 5
                                 "P 21.6997050461 17.1913408847 0 81 1\n" +
                                 "P 11.9501537874 0.0 1 46 5\n" +           // 7
                                 "P 10.1213155222 19.4653642205 1 46 5\n" + // 8
                                 "P 21.4509148631 7.06028751995 0 63 4\n" +
                                 "P 0.620554446576 12.4050767005 1 63 4\n" +// 10
                                 "P 1.14663348143 5.4910601421 1 47 4\n" +  // 11
                                 "P 20.9248358282 13.9743040784 0 47 4\n" +
                                 "P 12.471008807 2.25142379417 1 34 3\n" +  // 13
                                 "P 9.60046050261 17.2139404263 1 34 3\n" + // 14
                                 "P 22.0714693096 9.22576345354 0 89 3\n" +
                                 "P 0.0 10.2396007669 1 89 3\n" +           // 16
                                 "P 15.0592794423 4.36419105631 0 35 2\n" +
                                 "P 7.01218986729 15.1011731642 1 35 2\n" + // 18
                                 "P 13.3161012588 12.868126422 0 22 5\n" +
                                 "P 8.7553680508 6.59723779848 1 22 5\n" +  // 20
                                 "P 13.1966223118 16.1709472756 0 69 1\n" +
                                 "P 8.87484699782 3.29441694493 1 69 1\n" + // 22
                                 "go\n";
        
    @Test
    public void testDijkstra() {
        Game game = new Game(STATE);
        Dijkstra dijkstra = new Dijkstra(new Timer(), new BaseBot() {
            @Override
            public void doTurn() {
            }
        });
        
        dijkstra.calculate(game.planet(0), game.planets(Race.ALLY), null);
        
        assertEquals("10->18 transfer", 18, dijkstra.backEdge(game.planet(10)).id());
        assertEquals("22->20 transfer", 20, dijkstra.backEdge(game.planet(22)).id());
        
        int minBranchLength = Integer.MAX_VALUE;
        for (Planet p : game.planets(Race.ALLY)) {
            Planet backEdge = dijkstra.backEdge(p);
            if (backEdge == null)
                continue;
            if (p.distance(backEdge) < minBranchLength)
                minBranchLength = p.distance(backEdge);
        }
        assertEquals("check min branch length", 3, minBranchLength);
            
    }
    
}



