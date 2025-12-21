package biz.donvi.argsChecker;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for working with command maps.
 * Replaces the external biz.donvi.argsChecker library.
 */
public class Util {

    /**
     * Gets a nested map from the command tree by key.
     *
     * @param cmdMap The root command map
     * @param key The key to look up
     * @return The nested map, or an empty map if not found
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getImpliedMap(Map<String, Object> cmdMap, String key) {
        if (cmdMap == null) {
            return new HashMap<>();
        }

        Object value = cmdMap.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }

        return new HashMap<>();
    }
}
