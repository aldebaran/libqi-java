package com.aldebaran.qi;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * For convenience, this {@link FutureFunction} allows to implement 3 callbacks
 * independently for result, error or cancellation.
 * <p>
 * This allows to directly get the {@link Future}'s value as callback parameter.
 *
 * @param <Ret> the {@link Future}'s output type
 * @param <Arg> the {@link Future}'s input type
 */
public abstract class QiFunction<Ret, Arg> implements FutureFunction<Ret, Arg> {

    @Override
    public final Future<Ret> execute(Future<Arg> future) throws Throwable {
        try {
            return onResult(future.get());
        } catch (ExecutionException e) {
            return onError(e);
        } catch (CancellationException e) {
            return onCancel();
        }
    }

    public abstract Future<Ret> onResult(Arg result) throws Throwable;

    public Future<Ret> onError(Throwable error) throws Throwable {
        throw error;
    }

    public Future<Ret> onCancel() throws Throwable {
        return Future.cancelled();
    }
}
