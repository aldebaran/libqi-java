package com.aldebaran.qi;

/**
 * A {@code QiSlotException} is thrown when a slot could not be invoked due to a
 * missing {@link QiSlot} annotation,
 * an {@link java.lang.IllegalAccessException},
 * an {@link java.lang.IllegalArgumentException} or an
 * {@link java.lang.reflect.InvocationTargetException}.
 */
public class QiSlotException extends QiRuntimeException {
    /**
     * Constructs a {@code QiSlotException}
     */
    public QiSlotException() {
        // empty
    }

    /**
     * Constructs a {@code QiSlotException} with the specified detail message and
     * cause.
     *
     * @param message the detail message.
     * @param cause   the cause.
     */
    public QiSlotException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code QiSlotException} with the specified detail message.
     *
     * @param message the detail message.
     */
    public QiSlotException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code QiSlotException} with the specified cause.
     *
     * @param cause the cause.
     */
    public QiSlotException(Throwable cause) {
        super(cause);
    }
}
