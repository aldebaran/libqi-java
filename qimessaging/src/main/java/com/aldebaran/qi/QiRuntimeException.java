package com.aldebaran.qi;


/**
 * {@code QiRuntimeException} is a subclass of the standard Java
 * {@link java.lang.RuntimeException}. It is the superclass of those
 * <i>unchecked exceptions</i> that are linked to the qi Framework.
 */
@SuppressWarnings("serial")
public class QiRuntimeException extends RuntimeException {

    /**
     * Constructs a {@code QiRuntimeException}.
     */
    public QiRuntimeException() {
    }

    /**
     * Constructs a {@code QiRuntimeException} with the specified detail message
     * and cause.
     *
     * @param message the detail message.
     * @param cause   the cause.
     */
    public QiRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code QiRuntimeException} with the specified detail message.
     *
     * @param message the detail message.
     */
    public QiRuntimeException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code QiRuntimeException} with the specified cause.
     *
     * @param cause the cause.
     */
    public QiRuntimeException(Throwable cause) {
        super(cause);
    }
}
