package com.aldebaran.qi;

/**
 * Class used to provide a mechanism for chaining {@link Future}s by supplying
 * it to: {@link Future#andThen(Consumer)} and {@link Future#then(Consumer)}
 *
 * @param <T> the {@link Future}'s input type
 */
public interface Consumer<T> {
    /**
     * Performs this operation on the given argument.
     *
     * @param value the consumer argument
     * @throws Throwable any exception
     */
    void consume(T value) throws Throwable;
}
