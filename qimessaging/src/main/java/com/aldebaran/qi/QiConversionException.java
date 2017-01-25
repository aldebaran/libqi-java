package com.aldebaran.qi;

/**
 * A {@code QiConversionException} is an exception indicating that a custom
 * object could not be serialized and deserialized to or from a supported type.
 */
public class QiConversionException extends QiException {

    /**
     * Constructs a {@code QiConversionException}.
     */
    public QiConversionException() {
    }

    /**
     * Constructs a {@code QiConversionException} with the specified detail message
     * and cause.
     *
     * @param message the detail message.
     * @param cause   the cause.
     */
    public QiConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code QiConversionException} with the specified detail message.
     *
     * @param message the detail message.
     */
    public QiConversionException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code QiConversionException} with the specified cause.
     *
     * @param cause the cause.
     */
    public QiConversionException(Throwable cause) {
        super(cause);
    }
}
