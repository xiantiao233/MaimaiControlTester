package fun.xiantiao.maimaicontrol.utils;

public class PropertyUtil {

    public static int getProperty(String key, int def) {
        String property = System.getProperty(key);
        if (property == null) return def;
        try {
            return Integer.parseInt(property);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static boolean getProperty(String key, boolean def) {
        String property = System.getProperty(key);
        if (property == null) return def;
        if (key.equalsIgnoreCase("true")) return true;
        if (key.equalsIgnoreCase("false")) return false;
        return def;
    }
}
