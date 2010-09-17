package bot;

import shared.FutureOrder;
import shared.Planet;
import shared.Race;
import bot.algorithms.Expand;
import bot.algorithms.FastExpand;
import bot.algorithms.Profile;
import bot.algorithms.Reacquire;
import bot.algorithms.Reinforce;
import bot.algorithms.SneakAttack;

import compare.CumulativeCloseness;
import compare.IScore;
import compare.SimpleCloseness;
import compare.SimpleGrowthCloseness;

import filters.PlanetFilter;

public class TestingBot extends SimulatorBot {

    private static final int    EXTRA_SHIPS         = 1;

    IScore<Planet>      _allyGrowthCloseness;
    IScore<Planet>      _enemyGrowthCloseness;
    IScore<Planet>      _allySimpleCloseness;
    IScore<Planet>      _enemySimpleCloseness;
    IScore<Planet>      _cumulativeCloseness;
    
    // algorithms
    Profile             _profile;
    
    public static void main(String[] args) {
        parseArgs(args);
        SimulatorBot bot = new TestingBot();
        bot.run();
    }
    
    private TestingBot() {
        super();
        _profile = new Profile(this);
    }
    
    @Override
    public void doTurn() {
        super.doTurn();

        _allyGrowthCloseness  = new SimpleGrowthCloseness(_game.planets(Race.ALLY));
        _enemyGrowthCloseness = new SimpleGrowthCloseness(_game.planets(Race.ENEMY));
        _allySimpleCloseness  = new SimpleCloseness(_game.planets(Race.ALLY));
        _enemySimpleCloseness = new SimpleCloseness(_game.planets(Race.ENEMY));
        _cumulativeCloseness  = new CumulativeCloseness(_game.planets());
        
        if (_turn == 0 && new FastExpand(this).execute())
            return;

        carryOutFutureOrders();
        cleanupInvalidFutureOrders();
        
//        final PressureStatus pressureStatus = new PressureStatus(this);
//        pressureStatus.execute();
        
        new SneakAttack(this, _allyGrowthCloseness, EXTRA_SHIPS).execute();
        
        new Reacquire(this, _allyGrowthCloseness, EXTRA_SHIPS).execute();

        _profile.execute();

        new Expand(this, _allyGrowthCloseness, 
                   new PlanetFilter(_game.planets(Race.NEUTRAL)) {
                        @Override
                        public boolean filter(Planet planet) {
                            Planet future = _sim.simulate(planet, SimulatorBot.TURNS_PREDICT);
                            if (future.owner() != Race.NEUTRAL)
                                return false;
                            Planet closestEnemy = _adj.getNearestNeighbor(planet, Race.ENEMY);
                            Planet closestAlly  = _adj.getNearestNeighbor(planet, Race.ALLY);
                            if (closestAlly == null || closestEnemy == null)
                                return true;
                            if (closestAlly.distance(planet) > closestEnemy.distance(planet))
                                return false;
//                            return pressureStatus.canHold(planet);
                            return true;
                        } 
                   }.select(), 
                   EXTRA_SHIPS, true).execute();

        new Expand(this, _allySimpleCloseness, 
                new PlanetFilter(_game.planets(Race.ENEMY)) {
                     @Override
                     public boolean filter(Planet planet) {
                         Planet future = _sim.simulate(planet, SimulatorBot.TURNS_PREDICT);
                         if (future.owner() != Race.ENEMY)
                             return false;
                         return true;
                     } 
                }.select(), 
                EXTRA_SHIPS, true).execute();
        
        new Reinforce(this, _enemySimpleCloseness).execute();

        _profile.report();
    }

    public void cleanupInvalidFutureOrders() {
        log("# Future orders cleanup");
        for (Planet planet : _game.planets())
            _sim.simulate(planet, SimulatorBot.TURNS_PREDICT, true);
    }
    
    public void carryOutFutureOrders() {
        log("# advancing/carrying out future orders");
        _game.advanceFutureOrders();
        for (Planet planet : _game.planets(Race.ALLY))
            if (planet.hasFutureOrders(0)) {
                log("#\t issuing " + planet.futureOrders(0).size() + " future orders...");
                for (FutureOrder order : planet.futureOrders(0)) {
                    // try to issue all outgoing orders
                    if (order.from() == planet.id()) {
                        if (planet.ships() >= order.ships()) { 
                            issueOrder(_game.planet(order.from()), 
                                       _game.planet(order.to()), order.ships());
                            planet.addShips(-order.ships());
                            _game.removeArrival(order);                        
                        } else {
                            assert(reportInvalidOrder(order)) : "invalid future order after advance";
                        }
                    }
                }
                planet.removeFutureOrders(0);
            }
    }

    private boolean reportInvalidOrder(FutureOrder order) {
        log("#\t can't send future order: " + order);
        log("#\t source " + _game.planet(order.from()));
        return false;
    }
    
}
