/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;


/**
 * A {@code DynamicCallException} is thrown to indicate that an error occurred
 * in the messaging layer during a call.
 *
 * @see AnyObject
 */
@SuppressWarnings("serial")
public class DynamicCallException extends QiRuntimeException {

    /**
     * Constructs a {@code DynamicCallException} with the specified detail message.
     *
     * @param message the detail message.
     */
    public DynamicCallException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code DynamicCallException} with no detail message.
     */
    public DynamicCallException() {
        super();
    }

}
