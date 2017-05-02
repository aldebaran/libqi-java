/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

/**
 * An {@code AdvertisementException} is thrown when an error occurs during
 * the advertisement of a method, a signal or a property.
 *
 * @see DynamicObjectBuilder
 */
public class AdvertisementException extends QiRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an {@code AdvertisementException} with the specified detail
     * message.
     *
     * @param message the detail message.
     */
    public AdvertisementException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code AdvertisementException} with no specified detail message.
     */
    public AdvertisementException() {
        super();
    }
}
