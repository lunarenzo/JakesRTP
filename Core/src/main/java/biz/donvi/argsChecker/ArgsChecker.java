package biz.donvi.argsChecker;

import java.util.Arrays;

/**
 * Simple argument checker for command parsing.
 * Replaces the external biz.donvi.argsChecker library.
 */
public class ArgsChecker {

    private final String[] args;
    private int matchedCount = 0;

    public ArgsChecker(String[] args) {
        this.args = args != null ? args : new String[0];
    }

    /**
     * Checks if the arguments match the given pattern.
     * @param includeMatched If true, tracks how many args were matched for getRemainingArgs()
     * @param patterns The patterns to match. Use null for "any value", or a specific string for exact match.
     * @return true if the args match the pattern
     */
    public boolean matches(boolean includeMatched, String... patterns) {
        if (args.length < patterns.length) {
            return false;
        }

        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i] != null && !patterns[i].equalsIgnoreCase(args[i])) {
                return false;
            }
        }

        if (includeMatched) {
            matchedCount = patterns.length;
        }
        return true;
    }

    /**
     * Gets the remaining arguments after the matched portion.
     */
    public String[] getRemainingArgs() {
        if (matchedCount >= args.length) {
            return new String[0];
        }
        return Arrays.copyOfRange(args, matchedCount, args.length);
    }

    /**
     * Gets all arguments.
     */
    public String[] getArgs() {
        return args;
    }
}
