package biz.donvi.argsChecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for testing arguments against a command tree for tab completion.
 * Replaces the external biz.donvi.argsChecker library.
 */
public class ArgsTester {

    private static final String DYNAMIC_SUFFIX = " !dynamic";

    /**
     * Walks the command tree and returns completions for the current argument position.
     *
     * @param args The current command arguments
     * @param cmdMap The command tree map (loaded from YAML)
     * @param dynamicProvider Provider for dynamic suggestions
     * @return List of possible completions
     */
    @SuppressWarnings("unchecked")
    public static List<String> nextCompleteInTree(String[] args, Map<String, Object> cmdMap, DynamicArgsMap dynamicProvider) {
        List<String> completions = new ArrayList<>();

        if (args == null || args.length == 0) {
            // Return all top-level keys
            addKeysToCompletions(completions, cmdMap, "", dynamicProvider, new String[0]);
            return completions;
        }

        // Walk the tree
        Map<String, Object> currentMap = cmdMap;
        String[] pathSoFar = new String[0];

        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];
            Object next = findMatchingEntry(currentMap, arg);

            if (next instanceof Map) {
                currentMap = (Map<String, Object>) next;
                pathSoFar = appendToPath(pathSoFar, arg);
            } else {
                // Dead end - no completions
                return completions;
            }
        }

        // Now complete the last argument
        String partial = args[args.length - 1].toLowerCase();
        addKeysToCompletions(completions, currentMap, partial, dynamicProvider, pathSoFar);

        return completions;
    }

    @SuppressWarnings("unchecked")
    private static Object findMatchingEntry(Map<String, Object> map, String key) {
        // First try exact match
        if (map.containsKey(key)) {
            return map.get(key);
        }

        // Then try dynamic entries (keys ending with " !dynamic")
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey().endsWith(DYNAMIC_SUFFIX)) {
                return entry.getValue();
            }
        }

        return null;
    }

    private static void addKeysToCompletions(List<String> completions, Map<String, Object> map,
                                              String partial, DynamicArgsMap dynamicProvider, String[] path) {
        if (map == null) return;

        for (String key : map.keySet()) {
            if (key.endsWith(DYNAMIC_SUFFIX)) {
                // Dynamic entry - get suggestions from provider
                if (dynamicProvider != null) {
                    try {
                        String dynamicKey = key.substring(0, key.length() - DYNAMIC_SUFFIX.length());
                        dynamicProvider.getPotential(dynamicKey);
                        dynamicProvider.getPotential(path);
                        List<String> dynamic = dynamicProvider.getAndClearResult();
                        if (dynamic != null) {
                            for (String s : dynamic) {
                                if (s.toLowerCase().startsWith(partial)) {
                                    completions.add(s);
                                }
                            }
                        }
                    } catch (DynamicArgsMap.ResultAlreadySetException ignored) {
                    }
                }
            } else {
                // Static entry
                if (key.toLowerCase().startsWith(partial)) {
                    completions.add(key);
                }
            }
        }
    }

    private static String[] appendToPath(String[] path, String element) {
        String[] newPath = new String[path.length + 1];
        System.arraycopy(path, 0, newPath, 0, path.length);
        newPath[path.length] = element;
        return newPath;
    }
}
