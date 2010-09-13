package continuous;

import shared.Timer;
import shared.Utils;

public abstract class TimedWork {

    Timer   _timer;

    public TimedWork(Timer timer) {
        _timer = timer;
    }
    
    public void doWork(long limit) {
        if (isDone())
            return;
        do {
            doWorkChunk();
        } while (!isDone() && _timer.totalTime() < limit);
        Utils.log("# " + progress());
    }
    
    public abstract void doWorkChunk();
    
    public abstract boolean isDone();

    public abstract String progress();
    
}
