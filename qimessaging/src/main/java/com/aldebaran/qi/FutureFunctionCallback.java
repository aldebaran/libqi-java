package com.aldebaran.qi;

import java.util.concurrent.CancellationException;

/**
 * Class used to provide a mechanism for chaining {@link Future}s by supplying
 * it to: {@link Future#then(FutureFunction)}.
 *
 * @param <T> the {@link Future}'s input type
 */
public abstract class FutureFunctionCallback<T> implements FutureFunction<T, Void>  {
    @Override
    public final Void execute(Future<T> future) throws Throwable {
        try {
            onResult(future.get());
        }
        catch (CancellationException e) {
            onCancel();
        }
        catch (Exception e ){
            onError(e);
        }
        return null;
    }

    /**
     * Call when the future succeed with a value
     * @param value the value
     */
    public abstract void onResult(T value);

    /**
     * Call when the future is cancelled
     */
    public abstract void onCancel();

    /**
     * Call when the future failed with an error
     * @param throwable the exception
     */
    public abstract void onError(Throwable throwable);
}
