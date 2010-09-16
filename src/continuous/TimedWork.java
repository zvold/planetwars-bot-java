package continuous;

import shared.Timer;
import bot.BaseBot;

public abstract class TimedWork {

    Timer   _timer;
    BaseBot _bot;

    public TimedWork(Timer timer, BaseBot bot) {
        _timer = timer;
        _bot = bot;
    }
    
    public void doWork(long limit) {
        if (isDone())
            return;
        do {
            doWorkChunk();
        } while (!isDone() && _timer.totalTime() < limit);
        _bot.log("# " + progress());
    }
    
    public abstract void doWorkChunk();
    
    public abstract boolean isDone();

    public abstract String progress();
    
}
