package com.aldebaran.qi.util.resources;

public class ResourceFile extends ResourceElement {
    /**
     * File path
     */
    private final String path;

    /**
     * Create a new instance of ResourceFile
     *
     * @param path
     *            File path
     */
    public ResourceFile(String path) {
        if (path == null) {
            throw new NullPointerException("path MUST NOT be null");
        }

        path = path.trim();
        this.path = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    /**
     * File name <br>
     * <br>
     * <b>Parent documentation:</b><br>
     * {@inheritDoc}
     *
     * @return File name
     */
    @Override
    public String getName() {
        final int index = this.path.lastIndexOf('/');

        if (index < 0) {
            return this.path;
        }

        return this.path.substring(index + 1);
    }

    /**
     * File path <br>
     * <br>
     * <b>Parent documentation:</b><br>
     * {@inheritDoc}
     *
     * @return File path
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
     * @return {@code false}
     */
    @Override
    public boolean isDirectory() {
        return false;
    }
}