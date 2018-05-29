package com.aldebaran.qi.log;

/**
 * Interface to provide for catch logs
 */
public interface LogCatcher {
    /**
     * Called each time a log happen.</br>
     * It is called only for {@link LogLevel} the instance is registered
     * for.</br>
     * Don't do long operation inside this method. It may called from JNI C
     * side, and it is not a good idea to block it for long time. The system may
     * crash.
     *
     * @param logLevel
     *            Log level
     * @param message
     *            Log message
     */
    public void log(final LogLevel logLevel, final String message);
}
