/*
 * Copyright (C) 2019 Softbank Robotics Europe
 * See COPYING for the license
 */

package com.aldebaran.qi;

import com.aldebaran.qi.log.LogReport;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.aldebaran.qi.Algorithm.*;

/**
 * Class responsible for managing the dynamic libraries used by the qi Framework
 * according to the system.
 */
abstract class NativeLibrariesLoader {
    /**
     * Returns a loader depending on the current context.
     */
    private static NativeLibrariesLoader make() {
        return isInJar() ? new JarNativeLibrariesLoader() : new FileNativeLibrariesLoader();
    }

    /**
     * Find libraries based on a list of regex. The libraries have to be located in location directory.
     * If a library cannot be found, the function will simply skip it.
     * @param libraryRegexes List of all regex libraries.
     * @param location The resource path where libraries are located. It can either be a directory or a jar entry.
     */
    static void findAndLoadLibraries(List<String> libraryRegexes, String location)  {
        NativeLibrariesLoader instance = NativeLibrariesLoader.make();
        try {
            // List all available libraries at resource location.
            List<URL> availableLibraries = instance.listURLAtLocation(location);

            // Select required libraries based on regexes.
            List<URL> requiredLibraries = findLibraryThatMatch(availableLibraries, libraryRegexes);

            instance.loadURL(requiredLibraries);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Return as a List of URL all the sub elements at the provided location.
     * The location type described by the parameter depends of the ContextLoader instance.
     * i.e. If the instance is a JarContextLoader, the location refers to a jar file.
     */
    protected abstract List<URL> listURLAtLocation(String location) throws IOException;

    /**
     * Load all files in the list.
     * If an url does not refer to a valid file, the url will be skipped.
     */
    protected void loadURL(List<URL> libraries) {
        for(URL url : libraries) {
            String path = url.getPath();

            System.load(path);
            LogReport.information(String.format("%s loaded.\n", NativeLibrariesLoader.resourceName(url)));
        }
    }

    /**
     * Test if libqi-java package if located in a jar.
     */
    private static boolean isInJar() {
        return NativeLibrariesLoader.class.getResource("NativeLibrariesLoader.class").getProtocol().equals("jar");
    }

    /**
     * Find URL of a libqi-java package's resource.
     */
    private static URL findResourceURL(String resourceName) {
        return NativeLibrariesLoader.class.getResource('/' + resourceName);
    }

    /**
     * Get the name of the resource pointed by the given URL.
     */
    private static String resourceName(URL url){
        return new File(url.getFile()).getName();
    }

    /**
     * Return a predicate that tests if the name of resource at a URL matches the given regex.
     */
    private static Predicate<URL> urlMatch(final String regex) {
        return new Predicate<URL>() {
            @Override
            public boolean test(URL url) {
                return NativeLibrariesLoader.resourceName(url).matches(regex);
            }
        };
    }

    /**
     * For each regex, find the first library that matches it amongst the given url list.
     */
    private static List<URL> findLibraryThatMatch(final List<URL> availableLibraries, List<String> libraryRegexes) {
        List<URL> libraryNames = transform(libraryRegexes, new Function<String, URL>() {
            @Override
            public URL execute(String regex) {
                return findFirst(availableLibraries, urlMatch(regex));
            }
        });

        // If findFirst cannot find the library, null is returned. These values need to be filtered.
        return removeNull(libraryNames);
    }

    static class JarNativeLibrariesLoader extends NativeLibrariesLoader {
        private File tmpDirectory;

        JarNativeLibrariesLoader() {
            tmpDirectory = makeTmpDirectory();
        }

        private static String getTmpDirectoryPath() {
            return System.getProperty("java.io.tmpdir")
                    + File.separator + "qimessaging_lib";
        }

        private static File makeTmpDirectory() {
            File tmpDirectory = new File(getTmpDirectoryPath());

            tmpDirectory.mkdirs();
            tmpDirectory.deleteOnExit();

            return tmpDirectory;
        }

        /**
         * Return the jar file that contains libqi-java package.
         * @throws IOException if libqi-java package is not in a jar or jar is not accessible.
         */
        private static JarFile getJarFile() throws IOException {
            try {
                // The main problem is it is not possible to consistently get
                // the jar path from this class (see previous implementation
                // below).
                // 
                // Consequently, the solution is a bit complicated:
                // 1) get the fully-qualified name of this class.
                // 2) let the system search for the absolute path of this class,
                //    which contains the jar path.
                // 3) extract the jar path.

                // Previous implementation was
                // `NativeLibrariesLoader.class.getProtectionDomain().getCodeSource().getLocation()`,
                // but it is not consistent in all contexts (e.g. in Android's
                // version of Java).

                // 1)
                Class<?> qiClass = NativeLibrariesLoader.class;
                // Concatenate the package name and the class name the same way it is in the jar.
                String resourceName = qiClass.getPackage().getName().replace('.', '/')
                        + "/" + qiClass.getSimpleName() + ".class";

                // 2)
                String resourcePath = qiClass.getClassLoader().getResource(resourceName).getPath();

                // 3)
                // The resourcePath has the following pattern : `file:PATH_TO_JAR_FILE!/RESOURCE_NAME`.
                // We extract the jar file URL by removing `!/RESOURCE_NAME`.
                URL jarUrl = new URL(resourcePath.substring(0, resourcePath.lastIndexOf("!/" + resourceName)));
                return new JarFile(jarUrl.getPath());
            } catch (NullPointerException e) {
                // Rethrow the exception as IOException since the jar file is not accessible.
                throw new IOException(e);
            }
        }

        /**
         * Copy all bytes from input to output.
         */
        private static void copyStream(InputStream input, OutputStream output) throws IOException {
            final int BUFFER_SIZE = 4 * 1024;
            byte[] buffer = new byte[BUFFER_SIZE];
            int length; // Init at zero.
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
        }

        private static void closeQuietly(Closeable object) {
            try {
                if(Objects.nonNull(object) ) object.close();
            } catch (IOException ignored) {}
        }

        /**
         * Extract all resources in a temporary directory and return the URL of the extracted files.
         * @see #extractURLToFile(URL)
         */
        private List<URL> extractFromJar(List<URL> entries) {
            return transform(entries, new Function<URL, URL>() {
                @Override
                public URL execute(URL url) throws IOException {
                    return extractURLToFile(url);
                }
            });
        }

        /**
         * Extract the resource in a temporary directory and return the URL to the file.
         * @param url The URL to be extracted.
         * @return The URL of the file in the temporary directory.
         */
        private URL extractURLToFile(URL url) throws IOException {
            File destinationFile = new File(tmpDirectory, resourceName(url));

            destinationFile.createNewFile();

            InputStream input = null;
            OutputStream output = null;

            try{
                input = url.openStream();
                output = new FileOutputStream(destinationFile);

                copyStream(input, output);
            } finally {
                closeQuietly(input);
                closeQuietly(output);
            }

            return destinationFile.toURI().toURL();
        }

        /**
         * Generate a predicate that test if a jar entry is located in the specified directory.
         * This predicate accepts entries that are placed in sub folder in location directory.
         */
        private static Predicate<JarEntry> locatedIn(final String location) {
            return new Predicate<JarEntry>() {
                @Override
                public boolean test(JarEntry jarEntry) {
                    String jarStr = jarEntry.toString();
                    return jarStr.startsWith(location) && !jarStr.equals(location + '/');
                }
            };
        }

        @Override
        protected List<URL> listURLAtLocation(String location) throws IOException {
            // Select all entries in jar that are located in location directory.
            List<JarEntry> entries = Collections.list(getJarFile().entries());
            entries = findAll(entries, locatedIn(location));

            return transform(entries, new Function<JarEntry, URL>() {
                @Override
                public URL execute(JarEntry entry) {
                    return findResourceURL(entry.getName());
                }
            });
        }

        @Override
        protected void loadURL(List<URL> libraries) {
            // In a jar context, required libraries have to be first exported to a temporary directory.
            libraries = extractFromJar(libraries);

            super.loadURL(libraries);
        }
    }

    static class FileNativeLibrariesLoader extends NativeLibrariesLoader {

        /**
         * List files in libqi-java package's resource location.
         */
        private static List<File> listResourceFile(String location) {
            return Arrays.asList(new File(findResourceURL(location).getPath()).listFiles());
        }

        @Override
        protected List<URL> listURLAtLocation(String location) {
            return transform(listResourceFile(location), new Function<File, URL>() {
                @Override
                public URL execute(File file) throws MalformedURLException {
                    return file.toURI().toURL();
                }
            });
        }
    }
}
