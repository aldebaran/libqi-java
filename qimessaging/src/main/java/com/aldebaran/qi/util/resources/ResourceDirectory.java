package com.aldebaran.qi.util.resources;

/**
 * Resource directory
 */
public class ResourceDirectory extends ResourceElement {

    /**
     * Directory path
     */
    private final String path;

    /**
     * Create a new instance of ResourceDirectory
     *
     * @param path
     *            Directory path
     */
    public ResourceDirectory(String path) {
        if (path == null) {
            throw new NullPointerException("path MUST NOT be null");
        }

        path = path.trim();
        this.path = path + ((path.endsWith("/")) || (path.length() == 0) ? "" : "/");
    }

    /**
     * Directory name <br>
     * <br>
     * <b>Parent documentation:</b><br>
     * {@inheritDoc}
     *
     * @return Directory name
     */
    @Override
    public String getName() {
        if (this.path.length() < 2) {
            return this.path;
        }

        // Directory path end with /, so have to ignore it
        final int index = this.path.lastIndexOf('/', this.path.length() - 2);
        return this.path.substring(index + 1, this.path.length() - 1);
    }

    /**
     * Directory path <br>
     * <br>
     * <b>Parent documentation:</b><br>
     * {@inheritDoc}
     *
     * @return Directory path
     */
    @Override
    public String getPath() {
        return this.path;
    }

    /**
     * Indicates if its a directory <br>
     * <br>
     * <b>Parent documentation:</b><br>
     * {@inheritDoc}
     *
     * @return {@code true}
     */
    @Override
    public boolean isDirectory() {
        return true;
    }

    /**
     * Indicates if the directory is the resources root directory
     *
     * @return {@code true} if the directory is the resources root directory
     */
    public boolean isRoot() {
        return this.path.length() == 0;
    }
}