package tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import shared.FutureOrder;
import shared.Game;
import shared.Planet;
import shared.Race;
import simulate.LastLoseRecorder;
import simulate.Simulator;


public class FutureOrderTest {

    public static final String STATE = "P 11.8039955755 11.2157212798 0 37 3\n" +
                                       "P 9.31956732508 21.8088737532 1 16 5\n" +
                                       "P 14.2884238258 0.622568806433 2 16 5\n" +
                                       "P 11.8654926942 5.2737846552 0 81 3\n" +
                                       "P 11.7424984567 17.1576579045 0 81 3\n" +
                                       "P 4.25409258443 0.0 0 22 1\n" +
                                       "P 19.3538985665 22.4314425597 0 22 1\n" +
                                       "P 14.7436138612 22.3240014889 0 83 3\n" +
                                       "P 8.86437728973 0.107441070771 0 83 3\n" +
                                       "P 19.8543468498 0.711933891201 0 84 1\n" +
                                       "P 3.75364430115 21.7195086685 0 84 1\n" +
                                       "P 8.86481414847 9.73662367883 0 12 5\n" +
                                       "P 14.7431770024 12.6948188808 0 12 5\n" +
                                       "P 0.0 10.8098889721 0 59 2\n" +
                                       "P 23.6079911509 11.6215535875 0 59 2\n" +
                                       "P 20.3967683707 15.5228613809 0 59 3\n" +
                                       "P 3.21122278016 6.90858117873 0 59 3\n" +
                                       "P 17.0287479269 6.65976901033 0 4 2\n" +
                                       "P 6.57924322402 15.7716735493 0 4 2\n" +
                                       "P 0.782927536597 19.6075053882 0 55 1\n" +
                                       "P 22.8250636143 2.82393717142 0 55 1\n" +
                                       "P 2.60103334076 13.172383428 0 58 3\n" +
                                       "P 21.0069578101 9.25905913169 0 58 3\n" +
                                       "F 2 13 2 12 13 12\n" +
                                       "F 2 38 2 0 11 10\n" +
                                       "F 2 38 2 11 11 10\n" +
                                       "F 1 84 1 7 6 5\n" +
                                       "F 1 5 1 18 7 6\n" +
                                       "go\n";
    
    @Test
    public void testSimulatingFuture() {
        Game game = new Game();
        game.initFullState(STATE);
        
        Planet planet = game.planet(12);
        
        LastLoseRecorder lost = new LastLoseRecorder(Race.ENEMY);
        Simulator sim = new Simulator(game);
        sim.addListener(lost);
        Planet future = sim.simulate(planet, 12);   // enemy's fleet ETA
        assertEquals("check owner change", Race.ENEMY, future.owner());
        
        future = sim.simulate(planet, 11);          // enemy's fleet ETA minus 1
        assertEquals("check owner before arrival", Race.NEUTRAL, future.owner());

        future = sim.simulate(planet, 50);
        int enemyETA = lost.turn();                             // enemy's fleet ETA
        int shipsNeeded = lost.ships() + planet.growth() + 1;   // growth happens before arrival
        int allyETA = game.planet(1).distance(planet);          // allied ETA

        FutureOrder order = new FutureOrder(Race.ALLY, game.planet(1), planet, shipsNeeded, enemyETA + 1 - allyETA);
        game.addFutureOrder(order);
        
        LastLoseRecorder got = new LastLoseRecorder(Race.ALLY);
        sim.addListener(got);
        future = sim.simulate(planet, 13);
        assertEquals("check planet aquisition", true, got.changed());
        assertEquals("check planet aquisition's turn", 13, got.turn());
    }
    
}

