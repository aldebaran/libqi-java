package com.aldebaran.qi;

/**
 * Interface used to provide a mechanism for chaining {@link Future}s by supplying
 * it to: {@link Future#then(FutureFunction)} or {@link Future#andThen(FutureFunction)}.
 * <p>
 * For a simpler use, prefer using one of the two provided implementations:
 * {@link QiCallback} and {@link QiFunction}.
 *
 * @param <Ret> the {@link Future}'s output type
 * @param <Arg> the {@link Future}'s input type
 */
public interface FutureFunction<Ret, Arg> {
    Future<Ret> execute(Future<Arg> future) throws Throwable;
}
