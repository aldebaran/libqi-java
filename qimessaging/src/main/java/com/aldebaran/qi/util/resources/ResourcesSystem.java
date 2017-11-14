package com.aldebaran.qi.util.resources;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * System file like to see embed resources files as inside a browser.<br>
 * With it you can explore resources files tree.<br>
 * If resources are embed inside a jar only read operations are available, if
 * there are in outside directory read and write operations are possible.<br>
 * The path separator is /, what ever the system you are, conversions are made
 * for you.<br>
 * To have an instance use {@link Resources#obtainResourcesSystem()}
 */
public class ResourcesSystem {
    /**
     * Represents the resources files tree root
     */
    public static final ResourceDirectory ROOT = new ResourceDirectory("");
    /**
     * For read resources inside a jar
     */
    private final JarFile jarFile;
    /**
     * {@link Resources} reference to find resources
     */
    private final Resources resources;
    /**
     * Outside directory where lays the resources (if they are in outside
     * directory)
     */
    private final File rootFile;
    /**
     * Root path inside the jar (if resources are inside a jar)
     */
    private final String rootPath;

    /**
     * Create a new instance of ResourcesSystem in outside directory mode
     *
     * @param resources
     *            {@link Resources} reference to find resources
     * @param rootFile
     *            Outside root directory
     */
    ResourcesSystem(final Resources resources, final File rootFile) {
        this.resources = resources;
        this.rootFile = rootFile;
        this.jarFile = null;
        this.rootPath = null;
    }

    /**
     * Create a new instance of ResourcesSystem in jar embed mode
     *
     * @param resources
     *            {@link Resources} reference to find resources
     * @param jarFile
     *            Jar file that contains resources
     * @param rootPath
     *            Root opath inside the jar
     */
    ResourcesSystem(final Resources resources, final JarFile jarFile, final String rootPath) {
        this.resources = resources;
        this.rootFile = null;
        this.jarFile = jarFile;
        this.rootPath = rootPath;
    }

    /**
     * Obtain the parent of a resource element (file or directory).<br>
     * {@code null} is return for the parent of the root directory (By
     * definition root don't have parent)
     *
     * @param resourceElement
     *            Resource elemnt to have its parent directory
     * @return Resource directory parent or {@code null} if the elemnt is the
     *         root directory
     */
    public ResourceDirectory getParent(final ResourceElement resourceElement) {
        if (resourceElement == null) {
            throw new NullPointerException("resourceElement MUST NOT be null");
        }

        String path = resourceElement.getPath();

        if (path.length() == 0) {
            return null;
        }

        if (resourceElement.isDirectory()) {
            path = path.substring(0, path.length() - 1);
        }

        final int index = path.lastIndexOf('/');

        if (index < 0) {
            return ResourcesSystem.ROOT;
        }

        return new ResourceDirectory(path.substring(0, index + 1));
    }

    /**
     * Indicates if resources are inside a jar
     *
     * @return {@code true} if resources are inside a jar. {@code false} if
     *         resources are outside directory
     */
    public boolean insideJar() {
        return this.rootFile == null;
    }

    /**
     * Test if a resource element (file or directory) exists.
     *
     * @param resourceElement
     *            Resource element to test
     * @return {@code true} if element exists
     */
    public boolean isExists(final ResourceElement resourceElement) {
        if (resourceElement == null) {
            throw new NullPointerException("resourceElement MUST NOT be null");
        }

        if (this.rootFile != null) {
            final File file = new File(this.rootFile, resourceElement.getPath());
            return file.exists();
        }

        String name = this.rootPath;

        assert name != null;
        if (name.length() > 0) {
            name += "/";
        }

        name += resourceElement.getPath();

        assert this.jarFile != null;
        final JarEntry jarEntry = this.jarFile.getJarEntry(name);
        return jarEntry != null;
    }

    /**
     * Obtain a input stream for read a resource file
     *
     * @param resourceFile
     *            Resource file to read
     * @return Stream on the resource file
     */
    public InputStream obtainInputStream(final ResourceFile resourceFile) {
        if (resourceFile == null) {
            throw new NullPointerException("resourceFile MUST NOT be null");
        }

        return this.resources.obtainResourceStream(resourceFile.getPath());
    }

    /**
     * Obtain element at given path
     *
     * @param path
     *            Path element
     * @return The element
     */
    public ResourceElement obtainElement(String path) {
        String[] paths = path.split("/");
        ResourceElement resourceElement = ROOT;

        for (String name : paths) {
            if (name.length() > 0) {
                for (ResourceElement element : this.obtainList(resourceElement)) {
                    if (name.equals(element.getName())) {
                        resourceElement = element;
                        break;
                    }
                }
            }
        }

        return resourceElement;
    }

    /**
     * Obtain the list of resources elements child of an other resource element.<br>
     * If the resource element is a file, {@code null} is return because a file
     * can't have any children
     *
     * @param resourceElement
     *            Resource elemnt to have its children
     * @return List of children or {@code null} if given element is a file
     */
    public List<ResourceElement> obtainList(final ResourceElement resourceElement) {
        if (resourceElement == null) {
            throw new NullPointerException("resourceElement MUST NOT be null");
        }

        if (!resourceElement.isDirectory()) {
            return null;
        }

        return this.obtainList((ResourceDirectory) resourceElement);
    }

    /**
     * Obtain the list of resources elements (files or directories) inside a
     * resource directory
     *
     * @param resourceDirectory
     *            Resource directory to obtain its list of elements
     * @return List of resources elements inside the given directory
     */
    public List<ResourceElement> obtainList(final ResourceDirectory resourceDirectory) {
        if (resourceDirectory == null) {
            throw new NullPointerException("resourceDirectory MUST NOT be null");
        }

        final String path = resourceDirectory.getPath();
        final List<ResourceElement> list = new ArrayList<ResourceElement>();

        if (this.rootFile != null) {
            final File directory = new File(this.rootFile, path);
            String elementPath;
            final File[] content = directory.listFiles();

            if (content != null) {
                for (final File file : content) {
                    if (path.length() > 0) {
                        elementPath = path + file.getName();
                    }
                    else {
                        elementPath = file.getName();
                    }

                    if (file.isDirectory()) {
                        list.add(new ResourceDirectory(elementPath));
                    }
                    else {
                        list.add(new ResourceFile(elementPath));
                    }
                }
            }

            return list;
        }

        String start = this.rootPath;

        assert start != null;
        if (start.length() > 0) {
            start += "/";
        }

        final int indexRoot = start.length();

        if (path.length() > 0) {
            start += path;
        }

        final int min = start.length();
        int index;
        String name;
        final TreeSet<String> directories = new TreeSet<String>();

        assert this.jarFile != null;
        Enumeration<JarEntry> enumeration = this.jarFile.entries();
        JarEntry entry;

        while (enumeration.hasMoreElements()) {
            entry = enumeration.nextElement();
            name = entry.getName();

            if ((name.length() > min) && (name.startsWith(start))) {
                index = name.indexOf('/', min + 1);

                if ((index > 0) || ((index < 0) && (name.endsWith("/")))) {
                    if (index > 0) {
                        name = name.substring(indexRoot, index);
                    }
                    else {
                        name = name.substring(indexRoot);
                    }

                    if (directories.add(name)) {
                        list.add(new ResourceDirectory(name));
                    }
                }
                else if (index < 0) {
                    list.add(new ResourceFile(name.substring(indexRoot)));
                }

            }
        }

        return list;
    }
}