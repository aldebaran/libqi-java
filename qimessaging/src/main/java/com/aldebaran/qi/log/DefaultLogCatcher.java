package com.aldebaran.qi.log;

import java.io.PrintStream;

/**
 * Default log catcher, that publish log in standard stream
 *
 */
public final class DefaultLogCatcher implements LogCatcher {
    /** Default log catcher unique instance */
    public static final DefaultLogCatcher DEFAULT_LOG_CATCHER = new DefaultLogCatcher();

    private DefaultLogCatcher() {
    }

    /**
     * Called each time a log happen.</br>
     * It is called only for {@link LogLevel} the instance is registered for.
     *
     * @param logLevel
     *            Log level
     * @param message
     *            Log message
     */
    @Override
    public void log(final LogLevel logLevel, final String message) {
        if (logLevel == LogLevel.SILENT) {
            return;
        }

        final PrintStream printStream;

        if (logLevel == LogLevel.ERROR || logLevel == LogLevel.FATAL) {
            printStream = System.err;
        }
        else {
            printStream = System.out;
        }

        printStream.print(logLevel);
        printStream.print(": ");
        printStream.println(message);
    }
}