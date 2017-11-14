package com.aldebaran.qi.util.resources;

public abstract class ResourceElement {
    /**
     * Resource name
     *
     * @return Resource name
     */
    public abstract String getName();

    /**
     * Resource path
     *
     * @return Resource path
     */
    public abstract String getPath();

    /**
     * Indicates if the resource element is a directory
     *
     * @return {@code true} if the resource element is a directory
     */
    public abstract boolean isDirectory();
}
