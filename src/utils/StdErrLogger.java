package utils;

public class StdErrLogger implements ILogger {

    @Override
    public void log(String msg) {
        System.err.println(msg);
        System.err.flush();
    }

}
