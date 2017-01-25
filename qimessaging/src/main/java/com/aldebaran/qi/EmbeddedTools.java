/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import java.io.File;

/**
 * Class that provides the tools for loading the system's necessary
 * dynamic libraries (included in jar packages) used by the qi Framework.
 *
 * @author proullon
 */
public class EmbeddedTools {

    private File tmpDir = null;

    public static boolean LOADED_EMBEDDED_LIBRARY = false;

    private static native void initTypeSystem();

    /**
     * Override directory where native libraries are extracted.
     */
    public void overrideTempDirectory(File newValue) {
        SharedLibrary.overrideTempDirectory(newValue);

    }

    /**
     * Native C++ libraries are packaged with java sources.
     * This way, we are able to load them anywhere, anytime.
     */
    public boolean loadEmbeddedLibraries() {
        if (LOADED_EMBEDDED_LIBRARY == true) {
            System.out.print("Native libraries already loaded");
            return true;
        }

        String osName = System.getProperty("os.name");

        String javaVendor = System.getProperty("java.vendor");
        if (javaVendor.contains("Android")) {
            // Using System.loadLibrary will find the libraries automatically depending on the platform,
            // but we still need to load the dependencies manually and in the correct order.
            System.loadLibrary("gnustl_shared");
            System.loadLibrary("qi");
            System.loadLibrary("qimessagingjni");
        } else {
            // Windows is built with static boost libs,
            // but the name of the SSL dlls are different
            if (osName.contains("Windows")) {
                // Load vcredist libs
                SharedLibrary.loadLib("msvcr120");
                SharedLibrary.loadLib("msvcp120");
                SharedLibrary.loadLib("libeay32");
                SharedLibrary.loadLib("ssleay32");
            } else {
                SharedLibrary.loadLib("crypto");
                SharedLibrary.loadLib("ssl");
                SharedLibrary.loadLib("icudata"); // deps for boost_regexp.so
                SharedLibrary.loadLib("icuuc");
                SharedLibrary.loadLib("icui18n");
                SharedLibrary.loadLib("boost_atomic");
                SharedLibrary.loadLib("boost_date_time");
                SharedLibrary.loadLib("boost_system");
                SharedLibrary.loadLib("boost_thread");
                SharedLibrary.loadLib("boost_chrono");
                SharedLibrary.loadLib("boost_locale");
                SharedLibrary.loadLib("boost_filesystem");
                SharedLibrary.loadLib("boost_program_options");
                SharedLibrary.loadLib("boost_regex");
            } // linux, mac

            // Not on android, need to load qi and qimessagingjni
            if (SharedLibrary.loadLib("qi") == false
                    || SharedLibrary.loadLib("qimessagingjni") == false) {
                LOADED_EMBEDDED_LIBRARY = false;
                return false;
            }
        }

        System.out.printf("Libraries loaded. Initializing type system...\n");
        LOADED_EMBEDDED_LIBRARY = true;
        initTypeSystem();

        return true;
    }

}
