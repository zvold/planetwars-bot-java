package shared;

public class Timer {

    long _time;
    long _start;
    
    public void start() {
        _time = System.currentTimeMillis();
        _start = _time;
    }
    
    public long time() {
        long ret = System.currentTimeMillis() - _time;
        _time = System.currentTimeMillis();
        return ret;
    }

    public long total() {
        return System.currentTimeMillis() - _start;
    }
    
}
