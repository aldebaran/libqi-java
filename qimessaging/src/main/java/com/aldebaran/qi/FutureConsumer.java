package com.aldebaran.qi;

/**
 * Class used to provide a mechanism for chaining {@link Future}s by supplying
 * it to: {@link Future#then(FutureFunction)}.
 *
 * @param <T> the {@link Future}'s input type
 */
public interface FutureConsumer<T> {
    /**
     * Performs this operation on the given argument.
     *
     * @param future the function argument
     * @throws Throwable any exception
     */
    void consume(Future<T> future) throws Throwable;
}
