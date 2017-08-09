package com.aldebaran.qi;

/**
 * Interface used to provide a mechanism for chaining {@link Future}s by
 * supplying it to: {@link Future#andThenApply(Function)},
 * {@link Future#andThenCompose(Function)}, {@link Future#thenApply(Function)}
 * and {@link Future#thenCompose(Function)}
 * <p>
 * For a simpler use, prefer using {@link Consumer} implementation.
 *
 * @param <T>
 *            the {@link Future}'s input type
 * @param <R>
 *            the {@link Future}'s result type
 */
public interface Function<T, R> {
    /**
     * Apply this function to the given argument
     *
     * @param value
     *            the function argument
     * @return the function result
     * @throws Throwable
     **/
    R execute(T value) throws Throwable;
}
