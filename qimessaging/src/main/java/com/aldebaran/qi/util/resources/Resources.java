package com.aldebaran.qi.util.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.jar.JarFile;

/**
 * Access to internal resources
 */
public class Resources {
    /**
     * Reference class
     */
    private final Class<?> referenceClass;
    /**
     * Path relative to Resources class
     */
    private final String relativePathFormClass;
    /**
     * Resources system associated to the resources
     */
    private ResourcesSystem resourcesSystem;

    /**
     * Create a new instance of Resources with a reference class.<br>
     * The class reference must be in same jar as resources, all the given path
     * will be relative to this given class
     *
     * @param referenceClass
     *            Reference class
     */
    public Resources(final Class<?> referenceClass) {
        this.referenceClass = referenceClass;
        this.relativePathFormClass = null;
    }

    /**
     * Create a new instance of Resources with a relative base path.<br>
     * Resources to reach must be in same jar as this class, the path given is
     * relative to this Resources class. The path will be relative to the given
     * path.
     *
     * @param pathOfEmbedResources
     *            Relative path where found resources
     */
    public Resources(final String pathOfEmbedResources) {
        this.referenceClass = null;

        StringTokenizer stringTokenizer = new StringTokenizer(pathOfEmbedResources, "./\\:,;!|", false);
        final int numberPath = stringTokenizer.countTokens();
        final String[] path = new String[numberPath];

        for (int i = 0; i < numberPath; i++) {
            path[i] = stringTokenizer.nextToken();
        }

        stringTokenizer = new StringTokenizer(Resources.class.getPackage().getName(), "./\\:,;!|", false);
        final int numberPack = stringTokenizer.countTokens();
        final String[] pack = new String[numberPack];

        for (int i = 0; i < numberPack; i++) {
            pack[i] = stringTokenizer.nextToken();
        }

        final int limit = Math.min(numberPath, numberPack);
        int indexCommon = -1;

        for (int i = 0; i < limit; i++, indexCommon++) {
            if (!pack[i].equals(path[i])) {
                break;
            }
        }

        final StringBuilder stringBuilder = new StringBuilder();

        for (int i = numberPack - 1; i > indexCommon; i--) {
            stringBuilder.append("../");
        }

        for (int i = indexCommon + 1; i < numberPath; i++) {
            stringBuilder.append(path[i]);
            stringBuilder.append('/');
        }

        this.relativePathFormClass = stringBuilder.toString();
    }

    /**
     * Open stream to a resource
     *
     * @param path
     *            Relative path of the resource (Separator is "/")
     * @return Opened stream or null if the resource not found
     */
    public InputStream obtainResourceStream(final String path) {
        if (this.referenceClass != null) {
            return this.referenceClass.getResourceAsStream(path);
        }

        return Resources.class.getResourceAsStream(this.relativePathFormClass + path);
    }

    /**
     * URL of a resource
     *
     * @param path
     *            Relative path of the resource (Separator is "/")
     * @return URL or null if the resource not found
     */
    public URL obtainResourceURL(final String path) {
        if (this.referenceClass != null) {
            return this.referenceClass.getResource(path);
        }

        return Resources.class.getResource(this.relativePathFormClass + path);
    }

    /**
     * Obtain the resources system linked to the resources
     *
     * @return Resources system linked
     * @throws IOException
     *             If failed to create the resources system
     */
    public ResourcesSystem obtainResourcesSystem() throws IOException {
        if (this.resourcesSystem == null) {
            Class<?> clas = this.referenceClass;

            if (clas == null) {
                clas = Resources.class;
            }

            final URL url = clas.getResource(clas.getSimpleName() + ".class");
            String path = url.getFile();
            final String packageName = clas.getPackage().getName();

            if (packageName != null && packageName.length() > 0) {
                int count = 2;
                int index = packageName.indexOf('.');

                while (index >= 0) {
                    count++;
                    index = packageName.indexOf('.', index + 1);
                }

                for (; count > 0; count--) {
                    index = path.lastIndexOf('/');

                    if (index >= 0) {
                        path = path.substring(0, index);
                    }
                }
            }

            final int index = path.indexOf(".jar!");

            if (index < 0) {
                final File file = new File(path);
                this.resourcesSystem = new ResourcesSystem(this, file);
            }
            else {
                final JarFile jarFile = new JarFile(path.substring(5, index + 4));
                this.resourcesSystem = new ResourcesSystem(this, jarFile, "");
            }
        }

        return this.resourcesSystem;
    }
}