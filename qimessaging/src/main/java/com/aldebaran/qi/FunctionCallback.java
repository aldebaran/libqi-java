package com.aldebaran.qi;

/**
 * Class used to provide a mechanism for chaining {@link Future}s by supplying
 * it to: {@link Future#andThen(Function)}.
 *
 * @param <T> the {@link Future}'s input type
 */
public abstract class FunctionCallback<T> implements Function<T, Void> {
    @Override
    public final Void execute(T value) throws Throwable {
        onResult(value);
        return null;
    }

    /**
     * Call when the future succeed with a value
     * @param value the value
     */
    public abstract void onResult(T value);
}
