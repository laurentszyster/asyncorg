package org.async.core;

/**
 * A logger interface for unobtrusive and flexible logging.
 */
public interface Loginfo {
    /**
     * Logs out an uncategorized message string.
     * 
     * @param message to log out
     */
    public void out (String message);
    /**
     * Logs a message and category strings.
     * 
     * @param message to log
     * @param category of the message
     */
    public void err (String message, String category);
    /**
     * Logs an exception traceback.
     * 
     * @param error throwed
     */
    public void traceback (Throwable error);
}