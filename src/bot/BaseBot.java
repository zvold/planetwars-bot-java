package bot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

import shared.Fleet;
import shared.FutureOrder;
import shared.Game;
import shared.Planet;
import shared.Race;
import shared.Timer;
import shared.Utils;
import utils.ILogger;
import utils.StdErrLogger;

public abstract class BaseBot implements ILogger {

    int _turn;
    public Game _game;
    public Timer _timer = new Timer();
    ILogger _logger = new StdErrLogger();
    
    void run() {
        Scanner scanner = new Scanner(System.in);
        String line;
        _game = new Game();
        boolean parsing = false;
        try {
            while (scanner.hasNextLine()) {
                // start the timer pessimistically, not after 'go' command is received
                if (!parsing) {
                    parsing = true;
                    _timer.start();
                }
                line = scanner.nextLine();
                if (line.startsWith("go")) {
                    _game.resetTurn();
                    log("# parsing took " + _timer.time() + " ms");
                    parsing = false;
                    log("# doTurn(" + _turn + ") started at " + _timer.totalTime() + " ms total");
                    doTurn();
                    finishTurn();
                    log("# doTurn() took " + _timer.time() + " ms");
                    _turn++;
                } else {
                    _game.updateOneLine(line);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    public void issueOrder(Planet src, Planet dst, int ships) {
        assert(ships >= 0) : "invalid order";
        assert(src.ships() >= ships) : "invalid number of ships";
        if (ships == 0)
            return;
        _game.planet(dst.id()).addIncomingFleet(new Fleet(src.owner(), ships, src, dst));
        log("# sent " + ships + " ships " + src.id() + " -> " + dst.id());
        System.out.println(src.id() + " " + dst.id() + " " + ships);
        System.out.flush();
    }
    
    public void finishTurn() {
        System.out.println("go");
        System.out.flush();
    }
    
    public static void parseArgs(String[] args) {
        if (args.length > 0)
            Utils.parseTimeout(args[0]);
        if (args.length > 1)
            Utils.parseVerbose(args[1]);
    }

    @Override
    public void log(String msg) {
        if (!Utils._verbose)
            return;
        _logger.log(msg);
    }

    public void carryOutFutureOrders() {
        _game.advanceFutureOrders();
        boolean hasFutureOrders = false;
        for (Planet planet : _game.planets(Race.ALLY))
            if (planet.hasFutureOrders(0)) {
                if (!hasFutureOrders) {
                    hasFutureOrders = true;
                    log("# issuing " + planet.futureOrders(0).size() + " future orders...");
                }
                for (FutureOrder order : planet.futureOrders(0)) {
                    // try to issue all outgoing orders
                    if (order.from() == planet) {
                        if (planet.ships() >= order.ships()) { 
                            issueOrder(order.from(), order.to(), order.ships());
                            planet.addShips(-order.ships());
                            order.removeArrival();
                        } else {
                            // we're failing to send the order
                            log("# can't send future order: " + order);
                            // left it in place for outgoing planet, but remove for incoming one
                            FutureOrder removed = order.removeArrival();
                            log("# incoming order " +  removed + " removed for " + order.to());
                        }
                    }
                }
                planet.removeFutureOrders(0);
            }
        if (hasFutureOrders)
            log("# all future orders has been issued");
    }

    public int sumShips(Collection<Planet> planets) {
        int ret = 0;
        for (Planet planet : planets)
            ret += planet.ships();
        return ret;
    }

    public ArrayList<Planet> getNearestInRadius(Planet target, ArrayList<Planet> neighbors, 
                                                Race owner, int radius) {
        ArrayList<Planet> ret = new ArrayList<Planet>();
        ArrayList<Planet> allies = new ArrayList<Planet>();
        for (Planet planet : neighbors)
            if (planet.owner() == owner)
                allies.add(planet);

        if (!allies.isEmpty()) {
            Planet source = allies.get(0);
            ret.add(source);
            int distance = source.distance(target);
            for (int i = 1; i < allies.size(); i++) {
                Planet planet = allies.get(i);
                assert (planet.distance(target) >= distance) : "check sorting"
                        + planet.distance(target) + " >= " + distance;
                if (Math.abs(planet.distance(target) - distance) < radius)
                    ret.add(planet);
                else
                    break;
            }
        }
        assert(!ret.contains(target)) : "target doesn't belong to neighbors set";
        return ret;
    }
    
    public abstract void doTurn();

    
}
