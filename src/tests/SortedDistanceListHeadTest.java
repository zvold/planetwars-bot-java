package tests;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import shared.DistanceCache;
import shared.Game;
import shared.Planet;
import shared.Timer;
import shared.Utils;
import bot.BaseBot;
import continuous.Adjacency;
import filters.SortedDistanceListHead;

public class SortedDistanceListHeadTest {

    Random _rnd;
    
    @Before
    public void setUp() {
        _rnd = new Random(System.currentTimeMillis());
        DistanceCache.reset();
    }
    
    @Test
    public void testFilter() {
        Game game = new Game(GameStates.STATE17);
        Adjacency adj = getAdjacency(game);
        
        for (int i=0; i<50; i++)
            checkRandomFiltering(game, adj);
    }

    @Test
    public void testFilter2() {
        Game game = new Game(GameStates.STATE_BUG);
        Adjacency adj = getAdjacency(game);
        
        for (int i=0; i<50; i++)
            checkRandomFiltering(game, adj);
    }
    
    private Adjacency getAdjacency(Game game) {
        Adjacency adj = new Adjacency(game.planets(), new Timer(), new BaseBot() {
            @Override
            public void doTurn() {
            }
        });
        Utils.setLogging(false);
        while (!adj.isDone())
            adj.doWork(100);
        return adj;
    }

    private void checkRandomFiltering(Game game, Adjacency adj) {
        // pick up a 'target' planet at random
        Planet to = game.planet(_rnd.nextInt(game.planets().size()));
        
        // pick a threshold distance on random
        int than = to.distance(game.planet(_rnd.nextInt(game.planets().size())));
        
        ArrayList<Planet> result = new ArrayList<Planet>();
        SortedDistanceListHead filter = new SortedDistanceListHead(adj.neighbors(to), to, than);
        for (Planet p : filter)
            result.add(p);
        
        ArrayList<Planet> check = new ArrayList<Planet>();
        for (Planet p : adj.neighbors(to)) {
            if (p.distance(to) > than)
                break;
            check.add(p);
        }

        assertEquals("filter results compare, " + to + ", distance " + than, check, result);
    }
    
}
