package com.aldebaran.qi.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Capture log and permits to report them to a {@link LogCatcher}
 */
public class LogReport {
    /** Registered log catchers */
    private static final Map<LogLevel, List<LogCatcher>> CATCHERS = new HashMap<LogLevel, List<LogCatcher>>();
    /** Synchronization lock */
    private static final Object LOCK = new Object();

    /**
     * Internal register a {@link LogCatcher} for a log level.</br>
     * Must be called inside a "synchronized (LogReport.LOCK)" block
     *
     * @param logCatcher
     *            Log catcher to register
     * @param logLevel
     *            Log level to register for
     */
    private static void registerOneLevel(final LogCatcher logCatcher, final LogLevel logLevel) {
        assert logCatcher != null : "logCatcher must not be null!";
        assert logLevel != null : "logLevel must not be null!";
        assert logLevel != LogLevel.SILENT : "logLevel must not be LogLevel.SILENT!";

        List<LogCatcher> logCatchersList = LogReport.CATCHERS.get(logLevel);

        if (logCatchersList == null) {
            logCatchersList = new ArrayList<LogCatcher>();
            LogReport.CATCHERS.put(logLevel, logCatchersList);
        }

        if (!logCatchersList.contains(logCatcher)) {
            logCatchersList.add(logCatcher);
        }
    }

    /**
     * Internal unregister a {@link LogCatcher} for a log level.</br>
     * Must be called inside a "synchronized (LogReport.LOCK)" block
     *
     * @param logCatcher
     *            Log catcher to unregister
     * @param logLevel
     *            Log level to register for
     */
    private static void unregisterOneLevel(final LogCatcher logCatcher, final LogLevel logLevel) {
        assert logCatcher != null : "logCatcher must not be null!";
        assert logLevel != null : "logLevel must not be null!";
        assert logLevel != LogLevel.SILENT : "logLevel must not be LogLevel.SILENT!";

        final List<LogCatcher> logCatchersList = LogReport.CATCHERS.get(logLevel);

        if (logCatchersList != null) {
            logCatchersList.remove(logCatcher);

            if (logCatchersList.isEmpty()) {
                LogReport.CATCHERS.remove(logLevel);
            }
        }
    }

    /**
     * Fire log to all concerned {@link LogCatcher}
     *
     * @param logLevel
     *            Log level
     * @param message
     *            Log message
     */
    private static void fireLog(final LogLevel logLevel, final String message) {
        assert logLevel != null : "logLevel must not be null!";
        assert message != null : "message must not be null!";

        synchronized (LogReport.LOCK) {
            final List<LogCatcher> logCatchersList = LogReport.CATCHERS.get(logLevel);

            if (logCatchersList != null) {
                for (LogCatcher logCatcher : logCatchersList) {
                    // Since it may be called from JNI C context we can't allow
                    // exception happen here, so we capture it
                    try {
                        logCatcher.log(logLevel, message);
                    }
                    catch (final Throwable throwable) {
                        System.err.println("/!\\ Issue while report log inside : " + logCatcher.getClass().getName());
                        throwable.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Register a {@link LogCatcher} for specified log levels.</br>
     * For {@link LogLevel#SILENT} nothing is register, since its means no log
     * report.
     *
     * @param logCatcher
     *            {@link LogCatcher} to register, must not be {@code null}
     * @param logLevels
     *            Log levels to be alert. {@link LogLevel#SILENT} or
     *            {@code null} does nothing
     * @throws NullPointerException
     *             If given <b>logCatcher</b> is {@code null}
     */
    public static void register(final LogCatcher logCatcher, final LogLevel... logLevels) {
        if (logCatcher == null) {
            throw new NullPointerException("logCatcher must not be null!");
        }

        if (logLevels == null || logLevels.length == 0) {
            return;
        }

        synchronized (LogReport.LOCK) {
            for (LogLevel logLevel : logLevels) {
                if (logLevel == null || logLevel == LogLevel.SILENT) {
                    // Since silent is not report log we don't register anything
                    // for it
                    continue;
                }

                LogReport.registerOneLevel(logCatcher, logLevel);
            }
        }
    }

    /**
     * Register {@link LogCatcher} for given logLevel and all more important
     * that given one.</br>
     * Importance log level from more important to less is:
     * <ol>
     * <li>{@link LogLevel#FATAL}</li>
     * <li>{@link LogLevel#ERROR}</li>
     * <li>{@link LogLevel#WARNING}</li>
     * <li>{@link LogLevel#INFORMATION}</li>
     * <li>{@link LogLevel#VERBOSE}</li>
     * <li>{@link LogLevel#DEBUG}</li>
     * </ol>
     * Note that {@link LogLevel#SILENT} is a special level for no log at all
     *
     * @param logCatcher
     *            {@link LogCatcher} to register
     * @param logLevel
     *            Minimum level to capture
     * @throws NullPointerException
     *             If <b>logCatcher</b> or <b>logLevel</b> is {@code null}
     */
    public static void registerAtLeast(final LogCatcher logCatcher, LogLevel logLevel) {
        if (logCatcher == null) {
            throw new NullPointerException("logCatcher must not be null!");
        }

        if (logLevel == null) {
            throw new NullPointerException("logLevel must not be null!");
        }

        synchronized (LogReport.LOCK) {
            while (logLevel != LogLevel.SILENT) {
                LogReport.registerOneLevel(logCatcher, logLevel);
                logLevel = logLevel.moreImportant();
            }
        }
    }

    /**
     * Unregister a {@link LogCatcher} for specified log levels.</br>
     *
     * @param logCatcher
     *            {@link LogCatcher} to unregister, must not be {@code null}
     * @param logLevels
     *            Log levels to stop be alert.
     * @throws NullPointerException
     *             If given <b>logCatcher</b> is {@code null}
     */
    public static void unregister(final LogCatcher logCatcher, final LogLevel... logLevels) {
        if (logCatcher == null) {
            throw new NullPointerException("logCatcher must not be null!");
        }

        if (logLevels == null || logLevels.length == 0) {
            return;
        }

        synchronized (LogReport.LOCK) {
            for (LogLevel logLevel : logLevels) {
                if (logLevel == null || logLevel == LogLevel.SILENT) {
                    // Since silent is not report log we don't register anything
                    // for it
                    continue;
                }

                LogReport.unregisterOneLevel(logCatcher, logLevel);
            }
        }
    }

    /**
     * Register {@link LogCatcher} for given logLevel and all less important
     * that given one.</br>
     * Importance log level from more important to less is:
     * <ol>
     * <li>{@link LogLevel#FATAL}</li>
     * <li>{@link LogLevel#ERROR}</li>
     * <li>{@link LogLevel#WARNING}</li>
     * <li>{@link LogLevel#INFORMATION}</li>
     * <li>{@link LogLevel#VERBOSE}</li>
     * <li>{@link LogLevel#DEBUG}</li>
     * </ol>
     * Note that {@link LogLevel#SILENT} is a special level for no log at all
     *
     * @param logCatcher
     *            {@link LogCatcher} to register
     * @param logLevel
     *            Maximum level to stop to capture
     * @throws NullPointerException
     *             If <b>logCatcher</b> or <b>logLevel</b> is {@code null}
     */
    public static void unregisterAtMost(final LogCatcher logCatcher, LogLevel logLevel) {
        if (logCatcher == null) {
            throw new NullPointerException("logCatcher must not be null!");
        }

        if (logLevel == null) {
            throw new NullPointerException("logLevel must not be null!");
        }

        synchronized (LogReport.LOCK) {
            if (logLevel == LogLevel.SILENT) {
                logLevel = logLevel.lessImportant();
            }

            do {
                LogReport.unregisterOneLevel(logCatcher, logLevel);
                logLevel = logLevel.lessImportant();
            }
            while (logLevel != LogLevel.SILENT);
        }
    }

    /**
     * Unregister all log level that a given {@link LogCatcher} is registered
     * for
     *
     * @param logCatcher
     *            {@link LogCatcher} to unregister for
     * @throws NullPointerException
     *             If given <b>logCatcher</b> is {@code null}
     */
    public static void unregisterAll(final LogCatcher logCatcher) {
        if (logCatcher == null) {
            throw new NullPointerException("logCatcher must not be null!");
        }

        synchronized (LogReport.LOCK) {
            for (LogLevel logLevel : LogLevel.values()) {
                if (logLevel != LogLevel.SILENT) {
                    LogReport.unregisterOneLevel(logCatcher, logLevel);
                }
            }
        }
    }

    /**
     * Called by JNI C side to report captured log
     *
     * @param level
     *            Captured log level
     * @param message
     *            Captured log message
     */
    static void jniLog(final int level, final String message) {
        final LogLevel logLevel = LogLevel.obtainLogLevel(level);

        if (logLevel != LogLevel.SILENT) {
            LogReport.fireLog(logLevel, message);
        }
    }

    /**
     * Publish a log
     *
     * @param logLevel
     *            Log level
     * @param message
     *            Message to publish
     * @throws NullPointerException
     *             If <b>logLevel</b> or <b>message</b> is {@code null}
     */
    public static void log(LogLevel logLevel, String message) {
        if (logLevel == null) {
            throw new NullPointerException("logLevel must not be null!");
        }

        if (message == null) {
            throw new NullPointerException("message must not be null!");
        }

        if (logLevel != LogLevel.SILENT) {
            LogReport.fireLog(logLevel, message);
        }
    }

    /**
     * Publish a fatal log
     *
     * @param message
     *            Fatal message
     * @throws NullPointerException
     *             If <b>message</b> is {@code null}
     */
    public static void fatal(String message) {
        LogReport.log(LogLevel.FATAL, message);
    }

    /**
     * Publish a error log
     *
     * @param message
     *            Error message
     * @throws NullPointerException
     *             If <b>message</b> is {@code null}
     */
    public static void error(String message) {
        LogReport.log(LogLevel.ERROR, message);
    }

    /**
     * Publish a warning log
     *
     * @param message
     *            Warning message
     * @throws NullPointerException
     *             If <b>message</b> is {@code null}
     */
    public static void warning(String message) {
        LogReport.log(LogLevel.WARNING, message);
    }

    /**
     * Publish a information log
     *
     * @param message
     *            Information message
     * @throws NullPointerException
     *             If <b>message</b> is {@code null}
     */
    public static void information(String message) {
        LogReport.log(LogLevel.INFORMATION, message);
    }

    /**
     * Publish a verbose log
     *
     * @param message
     *            Verbose message
     * @throws NullPointerException
     *             If <b>message</b> is {@code null}
     */
    public static void verbose(String message) {
        LogReport.log(LogLevel.VERBOSE, message);
    }

    /**
     * Publish a debug log
     *
     * @param message
     *            Debug message
     * @throws NullPointerException
     *             If <b>message</b> is {@code null}
     */
    public static void debug(String message) {
        LogReport.log(LogLevel.DEBUG, message);
    }
}
