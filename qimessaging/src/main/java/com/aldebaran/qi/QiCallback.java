package com.aldebaran.qi;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * Like {@link QiFunction}, this {@link FutureFunction} allows to implement 3
 * callbacks independently for result, error or cancellation.
 * <p>
 * Contrary to {@link QiFunction}, all these callback methods return void.
 * <p>
 * This is a facility to register a callback to a {@link Future}, without the
 * need to manage what to return (that will be ignored anyway).
 *
 * @param <T> the {@link Future}'s type.
 */
public abstract class QiCallback<T> implements FutureFunction<Void, T> {
    @Override
    public final Future<Void> execute(Future<T> future) throws Throwable {
        try {
            onResult(future.get());
            return null;
        } catch (ExecutionException e) {
            onError(e);
            return null;
        } catch (CancellationException e) {
            onCancel();
            return Future.cancelled();
        }
    }

    public abstract void onResult(T result) throws Throwable;

    public void onError(Throwable error) throws Throwable {
      throw error;
    }

    public void onCancel() throws Throwable {
        // do nothing
    }
}
