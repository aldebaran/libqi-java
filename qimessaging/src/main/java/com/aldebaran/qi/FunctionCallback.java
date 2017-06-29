package com.aldebaran.qi;

/**
 * Created by epinault on 29/06/2017.
 */
public abstract class FunctionCallback<T> implements Function<T, Void> {
    @Override
    public final Void execute(T value) throws Throwable {
        onResult(value);
        return null;
    }

    public abstract void onResult(T value);
}
