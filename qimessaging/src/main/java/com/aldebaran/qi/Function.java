package com.aldebaran.qi;

/**
 * Interface used to provide a mechanism for chaining {@link Future}s by supplying
 * it to: {@link Future#andThen(Function)}.
 * <p>
 * For a simpler use, prefer using one of the two provided implementations:
 *
 * @param <R> the {@link Future}'s output type
 * @param <T> the {@link Future}'s input type
 */
public interface Function<T, R> {
    R execute(T value) throws Throwable;
}
