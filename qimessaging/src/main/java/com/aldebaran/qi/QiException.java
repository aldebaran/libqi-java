package com.aldebaran.qi;


/**
 * {@code QiException} is a subclass of the standard Java
 * {@link java.lang.Exception}. It is the superclass of those <i>checked
 * exceptions</i> that are linked to the qi Framework.
 */
@SuppressWarnings("serial")
public class QiException extends Exception {
    /**
     * Constructs a {@code QiException}.
     */
    public QiException() {
    }

    /**
     * Constructs a {@code QiException} with the specified detail message
     * and cause.
     *
     * @param message the detail message.
     * @param cause   the cause.
     */
    public QiException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code QiException} with the specified detail message.
     *
     * @param message the detail message.
     */
    public QiException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code QiException} with the specified cause.
     *
     * @param cause the cause.
     */
    public QiException(Throwable cause) {
        super(cause);
    }
}
