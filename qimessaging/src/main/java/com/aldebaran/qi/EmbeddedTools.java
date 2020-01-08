/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import com.aldebaran.qi.log.LogLevel;
import com.aldebaran.qi.log.LogReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * Class that provides the tools for loading the system's necessary
 * dynamic libraries (included in jar packages) used by the qi Framework.
 *
 * @author proullon
 */
class EmbeddedTools {
    /// Static constant definition

    /**
     * Loader log prefix message.
     */
    private static final String LOADER_LOG_PREFIX = "qi native libraries";

    /// Native method definition

    /**
     * Libqi-java jni library type system initialisation.
     */
    static native void initTypeSystem();

    /// Static instance definition
    private static AtomicBoolean embeddedLibrariesLoaded = new AtomicBoolean();

    private static void log(LogLevel level, String message) {
        LogReport.log(level, String.format("{}: {}", LOADER_LOG_PREFIX, message));
    }

    /**
     * Finds the resource directory name depending the on the os type.
     */
    private static String getResourceDirectoryName() {
        switch (SystemUtil.osType) {
            case Linux :
                return "linux64";
            case Mac:
                return "mac64";
            case Windows:
                return "win64";
            default:
                return null;
        }
    }

    /**
     * Produce a regex that matches library names depending on the operating system library name convention.
     */
    private static String defaultRegex(String name) {
        return Pattern.quote(System.mapLibraryName(name));
    }

    /**
     * Produce a regex that matches Windows shared libraries name.
     * This regex will reject libraries containing "_d" between name and extension.
     */
    static String winSystemRegex(String name) {
        return Pattern.quote(name) + "((?!_d).)*" + Pattern.quote(".dll");
    }

    /**
     * Produce a regex that matches UNIX shared libraries name.
     */
    static String unixSystemRegex(String name) {
        return Pattern.quote(System.mapLibraryName(name)) + ".*";
    }

    /**
     * Produce a platform agnostic regex that maches boost libraries name.
     */
    static String boostRegex(String name) {
        if (SystemUtil.IS_OS_WINDOWS)
            return Pattern.quote(name) + "((?!gd).)*" + Pattern.quote(".dll");
        else
            return unixSystemRegex(name);
    }

    /**
     * Generate libqi-java dependency regex list.
     */
    static List<String> getDependencyRegexList() {
        List<String> libs = new ArrayList<String>();

        if (SystemUtil.IS_OS_WINDOWS) {
            libs.addAll(Algorithm.transform(Arrays.asList(
                    "vcruntime",
                    "libeay",
                    "concrt",
                    "msvcp",
                    "ssleay"
            ), new Function<String, String>() {
                @Override
                public String execute(String name) {
                    return winSystemRegex(name);
                }
            }));

        } else if (SystemUtil.IS_OS_MAC || SystemUtil.IS_OS_LINUX) {
            libs.addAll(Algorithm.transform(Arrays.asList(
                    "crypto",
                    "ssl",
                    "icudata",
                    "icuuc",
                    "icui18n"
            ), new Function<String, String>() {
                @Override
                public String execute(String name) {
                    return unixSystemRegex(name);
                }
            }));
        }

        libs.addAll(Algorithm.transform(Arrays.asList(
                "boost_system",
                "boost_random",
                "boost_chrono",
                "boost_thread",
                "boost_regex",
                "boost_program_options",
                "boost_locale",
                "boost_filesystem",
                "boost_date_time"
        ), new Function<String, String>() {
            @Override
            public String execute(String name) {
                return boostRegex(name);
            }
        }));

        libs.addAll(Algorithm.transform(Arrays.asList(
                "qi",
                "qimessagingjni"
        ), new Function<String, String>() {
            @Override
            public String execute(String name) {
                return defaultRegex(name);
            }
        }));

        return libs;
    }

    static Result<Unit, Throwable> tryLoadLibrary(String libraryName) {
        try {
            System.loadLibrary(libraryName);
            return Result.of(Unit.value());
        } catch (Throwable e) {
            return Result.error(e);
        }
    }

    /**
     * Loads native android libraries.
     */
    static void tryLoadAndroidLibraries() {
        log(LogLevel.VERBOSE, "attempting to load c++ standard library.");

        // Using System.loadLibrary will find the libraries automatically depending on the platform,
        // but we still need to load the dependencies manually and in the correct order.

        try {
            tryLoadLibrary("c++_shared")
                .ifErrPresent(new Consumer<Throwable>() {
                @Override
                public void consume(final Throwable cppLoadError) throws Throwable {
                    tryLoadLibrary("gnustl_shared")
                        .ifErrPresent(new Consumer<Throwable>() {
                        @Override
                        public void consume(Throwable gnuLoadError) {
                            throw new UnsatisfiedLinkError(String.format(
                                    "{}: unable to load c++ standard library, c++_shared reason: {}, gnustl_shared reason: {}",
                                    LOADER_LOG_PREFIX, cppLoadError.getMessage(), gnuLoadError.getMessage()
                            ));
                        }
                    });
                }
            });
        // Catch checked exceptions (`Throwable`) in order to avoid making `tryLoadAndroidLibraries` throwable
        // but rethrow all unchecked exceptions.
        }
        catch (Error error) {
            throw error;
        }
        catch (RuntimeException exception) {
            throw  exception;
        }
        catch (Throwable throwable) {
            // If the exception is checked, rethrow it as a RunTimeException which is unchecked.
            throw new RuntimeException(throwable);
        }

        System.loadLibrary("qi");
        System.loadLibrary("qimessagingjni");
    }

    /**
     * Detects current system and loads matching native libraries.
     */
    static void tryLoadLibraries() {
        String javaVendor = System.getProperty("java.vendor");
        if (javaVendor.contains("Android")) {
            tryLoadAndroidLibraries();
        } else {
            NativeLibrariesLoader.findAndLoadLibraries(getDependencyRegexList(), getResourceDirectoryName());
        }
    }

    static void loadEmbeddedLibraries() {
        if (embeddedLibrariesLoaded.compareAndSet(false, true)) {
            log(LogLevel.INFORMATION, "starting native libraries loading.");
            tryLoadLibraries();
            log(LogLevel.INFORMATION, "starting type system initialization.");
            initTypeSystem();
            log(LogLevel.INFORMATION, "type system initialized.");
        }
    }

}
