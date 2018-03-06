package com.aldebaran.qi.log;

/**
 * Level of log.</br>
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
 */
public enum LogLevel {
    /** No log should be display at this level */
    SILENT(0),
    /** So important message that it is consider as fatal when show it */
    FATAL(1),
    /** Log appear when error happen */
    ERROR(2),
    /** Warn developer of something may be dangerous */
    WARNING(3),
    /** Informative debug */
    INFORMATION(4),
    /** Log not important */
    VERBOSE(5),
    /** Debugging purpose log */
    DEBUG(6);

    /**
     * Obtain {@link LogLevel} corresponding to given level.</br>
     * If no corresponding level, {@link LogLevel#SILENT} is returned
     *
     * @param level
     *            Level to search its corresponding {@link LogLevel}
     * @return Corresponding {@link LogLevel}
     */
    public static LogLevel obtainLogLevel(final int level) {
        for (LogLevel logLevel : LogLevel.values()) {
            if (logLevel.level == level) {
                return logLevel;
            }
        }

        return LogLevel.SILENT;
    }

    /** Log level */
    private final int level;

    /**
     * Create log level
     *
     * @param level
     *            Log level
     */
    LogLevel(final int level) {
        this.level = level;
    }

    /**
     * Log level order.</br>
     * More level is low, more log is important
     *
     * @return Log level order
     */
    public int level() {
        return this.level;
    }

    /**
     * Obtain level more important in hierarchy
     *
     * @return More important level
     */
    public LogLevel moreImportant() {
        return LogLevel.obtainLogLevel(this.level - 1);
    }

    /**
     * Obtain level less important in hierarchy
     *
     * @return Less important level
     */
    public LogLevel lessImportant() {
        return LogLevel.obtainLogLevel(this.level + 1);
    }
}
