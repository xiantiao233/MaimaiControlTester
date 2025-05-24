package fun.xiantiao.maimaicontrol.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DebugUtil {

    private static final Logger logger = LogManager.getLogger("Debug");
    private static final boolean debug = PropertyUtil.getProperty("fun.xiantiao.Debug", false);

    public static <T> T debugData(T data){
        if (debug) logger.info(String.valueOf(data));
        return data;
    }
}
