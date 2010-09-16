package bot;

import java.util.Scanner;

import shared.Game;
import shared.Planet;
import shared.Timer;
import shared.Utils;
import utils.ILogger;
import utils.StdErrLogger;

public abstract class BaseBot implements ILogger {

    Game _game;
    Timer _timer = new Timer();
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
                    doTurn();
                    finishTurn();
                    log("# doTurn() took " + _timer.time() + " ms");
                } else {
                    _game.updateOneLine(line);
                }
            }
        } catch (Throwable e) {
            // send a message to the engine in hope to see it in the log
            System.out.println(e);
            System.out.flush();
        }
    }
    
    public void issueOrder(Planet src, Planet dst, int ships) {
        assert(ships >= 0) : "invalid order";
        assert(src.ships() >= ships) : "invalid number of ships";
        if (ships == 0)
            return;
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
    
    public abstract void doTurn(); 
    
}
