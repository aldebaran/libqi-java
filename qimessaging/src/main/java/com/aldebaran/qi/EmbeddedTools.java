/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

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

    /// Native method definition

    /**
     * Libqi-java jni library type system initialisation.
     */
    static native void initTypeSystem();

    /// Static instance definition
    private static AtomicBoolean embeddedLibrariesLoaded = new AtomicBoolean();

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

    /**
     * Loads native libraries.
     */
    static void tryLoadLibraries() {
        String javaVendor = System.getProperty("java.vendor");
        if (javaVendor.contains("Android")) {
            // Using System.loadLibrary will find the libraries automatically depending on the platform,
            // but we still need to load the dependencies manually and in the correct order.
            System.loadLibrary("gnustl_shared");
            System.loadLibrary("qi");
            System.loadLibrary("qimessagingjni");
        } else {
            NativeLibrariesLoader.findAndLoadLibraries(getDependencyRegexList(), getResourceDirectoryName());
        }
    }

    static void loadEmbeddedLibraries() {
        if (embeddedLibrariesLoaded.compareAndSet(false, true)) {
            tryLoadLibraries();
            LogReport.information("Libraries loaded. Initializing type system...");
            initTypeSystem();
        }
    }

}
