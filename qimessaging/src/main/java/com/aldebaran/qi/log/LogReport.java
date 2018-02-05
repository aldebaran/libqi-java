package com.aldebaran.qi.log;

public class LogReport {
    /*
     * enum LogLevel { LogLevel_Silent = 0, ///< silent log level
     * LogLevel_Fatal, ///< fatal log level
     * LogLevel_Error, ///< error log level
     * LogLevel_Warning, ///< warning log level
     * LogLevel_Info, ///< info log
     * level LogLevel_Verbose, ///< verbose log level
     * LogLevel_Debug ///< debug
     * log level };
     */
    static void jniLog(int logLevel, String message) {
        System.out.println("NONO-LogReport: logLevel=" + logLevel + " | message=" + message);
    }
}
