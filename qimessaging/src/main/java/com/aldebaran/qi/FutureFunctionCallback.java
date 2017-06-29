package com.aldebaran.qi;

import java.util.concurrent.CancellationException;

/**
 * Created by epinault on 29/06/2017.
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

    public abstract void onResult(T value);

    public abstract void onCancel();

    public abstract void onError(Throwable throwable);
}
