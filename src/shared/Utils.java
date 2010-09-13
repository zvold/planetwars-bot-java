package shared;

public class Utils {

    public static boolean _verbose = false;
    public static int _timeout = 1000;
    
    public static void log(String msg) {
        if (!_verbose)
            return;
        System.err.println(msg);
        System.err.flush();
    }

    public static void setLogging(boolean flag) {
        _verbose = flag;
    }
    
    public static int timeout() {
        return _timeout;
    }

    public static void parseTimeout(String line) {
        _timeout = Integer.parseInt(line);
    }
    
    public static void parseVerbose(String line) {
        _verbose = Boolean.parseBoolean(line);
    }
    
}
