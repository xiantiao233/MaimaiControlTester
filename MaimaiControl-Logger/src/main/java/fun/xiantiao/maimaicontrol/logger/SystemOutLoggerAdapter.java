package fun.xiantiao.maimaicontrol.logger;

public class SystemOutLoggerAdapter implements UniversalLogger {

    @Override
    public void info(String message) {
        System.out.println("[INFO] " + message);
    }

    @Override
    public void warn(String message) {
        System.out.println("[WARN] " + message);
    }

    @Override
    public void error(String message) {
        System.err.println("[ERROR] " + message);
    }

    @Override
    public void debug(String message) {
        System.out.println("[DEBUG] " + message);
    }

    @Override
    public void trace(String message) {
        System.out.println("[TRACE] " + message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        System.err.println("[ERROR] " + message);
        throwable.printStackTrace(System.err);
    }
}
