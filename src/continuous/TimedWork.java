package continuous;

import shared.Timer;
import utils.ILogger;

public abstract class TimedWork {

    Timer   _timer;
    ILogger _logger;

    public TimedWork(Timer timer, ILogger logger) {
        _timer = timer;
        _logger = logger;
    }
    
    public void doWork(long limit) {
        if (isDone())
            return;
        do {
            doWorkChunk();
        } while (!isDone() && _timer.total() < limit);
        _logger.log("# " + progress());
    }
    
    public abstract void doWorkChunk();
    
    public abstract boolean isDone();

    public abstract String progress();
    
}
