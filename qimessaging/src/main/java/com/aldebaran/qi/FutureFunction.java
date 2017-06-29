package com.aldebaran.qi;

/**
 * Interface used to provide a mechanism for chaining {@link Future}s by supplying
 * it to: {@link Future#then(FutureFunction)}.
 * <p>
 * For a simpler use, prefer using {@link FutureFunctionCallback} implementation.
 *
 * @param <T> the {@link Future}'s input type
 * @param <R> the {@link Future}'s result type
 */
public interface FutureFunction<T, R> {
    /**
     * Apply this function to the given argument
     *
     * @param future the function argument
     * @return the function result
     * @throws Throwable
     */
    R execute(Future<T> future) throws Throwable;
}
