package fun.xiantiao.maimaicontrol.logger;

public interface UniversalLogger {

    void info(String message);

    void warn(String message);

    void error(String message);

    void debug(String message);

    void trace(String message);

    void error(String message, Throwable throwable);

}
