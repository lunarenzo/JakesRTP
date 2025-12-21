package biz.donvi.argsChecker;

import java.util.List;

/**
 * Abstract class for providing dynamic argument suggestions for tab completion.
 * Replaces the external biz.donvi.argsChecker library.
 */
public abstract class DynamicArgsMap {

    private List<String> result = null;

    /**
     * Called to get potential values for a path represented as an array.
     * Implementations should call setResult() to provide suggestions.
     */
    public abstract void getPotential(String[] path) throws ResultAlreadySetException;

    /**
     * Called to get potential values for a single path element.
     * Implementations should call setResult() to provide suggestions.
     */
    public abstract void getPotential(String path) throws ResultAlreadySetException;

    /**
     * Sets the result for tab completion.
     */
    protected void setResult(List<String> result) throws ResultAlreadySetException {
        if (this.result != null) {
            throw new ResultAlreadySetException();
        }
        this.result = result;
    }

    /**
     * Gets and clears the result.
     */
    public List<String> getAndClearResult() {
        List<String> r = result;
        result = null;
        return r;
    }

    /**
     * Exception thrown when setResult is called twice.
     */
    public static class ResultAlreadySetException extends Exception {
        public ResultAlreadySetException() {
            super("Result has already been set");
        }
    }
}
