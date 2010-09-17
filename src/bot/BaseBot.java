package bot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import shared.Fleet;
import shared.Game;
import shared.Planet;
import shared.Race;
import shared.Timer;
import shared.Utils;
import utils.ILogger;
import utils.StdErrLogger;
import filters.OwnerFilter;
import filters.PlanetFilter;

public abstract class BaseBot implements ILogger {

    int _turn;
    public Game _game = new Game();
    public Timer _timer = new Timer();
    ILogger _logger = new StdErrLogger();
    
    void run() {
        Scanner scanner = new Scanner(System.in);
        String line;
        boolean parsing = false;
        try {
            while (scanner.hasNextLine()) {
                // start the timer pessimistically, not after 'go' command is received
                if (!parsing) {
                    parsing = true;
                    _timer.start();
                    _game.startTurnParsing();
                }
                line = scanner.nextLine();
                if (line.startsWith("go")) {
                    log("# parsing took " + _timer.time() + " ms");
                    parsing = false;
                    log("# doTurn(" + _turn + ") started at " + _timer.total() + " ms total");
                    doTurn();
                    finishTurn();
                    log("# doTurn() took " + _timer.time() + " ms");
                    _turn++;
                    if (_turn % Utils.gcturn() == 0) {
                        // looks like a good time for GC
                        System.gc();
                        log("# System.gc() took " + _timer.time() + " ms");
                    }
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
        dst.addIncomingFleet(new Fleet(src.owner(), ships, src, dst));
        log("#\t\t sent " + ships + " ships " + src.id() + " -> " + dst.id());
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

    public int sumShips(Collection<Planet> planets) {
        int ret = 0;
        for (Planet planet : planets)
            ret += planet.ships();
        return ret;
    }

    public List<Planet> getNearestInRadius(final Planet target, ArrayList<Planet> neighbors, 
                                           Race owner, final int radius) {
        List<Planet> ret = new ArrayList<Planet>();
        List<Planet> allies = new OwnerFilter(neighbors, owner).select();

        if (allies.isEmpty())
            return ret;
        final Planet source = allies.remove(0);
        final int distance = source.distance(target);
        ret.add(source);
        ret.addAll(new PlanetFilter(allies) {
            @Override
            public boolean filter(Planet planet) {
                assert (planet.distance(target) >= distance) : "check sorting"
                        + planet.distance(target) + " >= " + distance;
                return (Math.abs(planet.distance(target) - distance) < radius);
            }
        }.select());
        assert(!ret.contains(target)) : "target doesn't belong to neighbors set";
        return ret;
    }
    
    public abstract void doTurn();
    
}
