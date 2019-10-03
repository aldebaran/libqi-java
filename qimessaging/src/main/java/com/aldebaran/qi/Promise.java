package com.aldebaran.qi;

/**
 * Promise is a writable, single assignment container which sets the value of
 * the {@link Future}.
 * <p>
 * Promise and {@link Future} are two complementary concepts. They are designed
 * to synchronize data between multiples threads. The {@link Future} holds the
 * result of an asynchronous computation, from which you can retrieve the value
 * of the result; the Promise sets the value of this computation, which resolves
 * the associated {@link Future}.
 * <p>
 * For promises that don't need a value and are just used to ensure correct
 * ordering of asynchronous operations, the common pattern to use is
 * {@link java.lang.Void} as a generic type.
 * <p>
 * Warning:
 * After a call of {@link #setValue(Object)}, {@link #setError(String)} or {@link #setCancelled()},
 * it is illegal to call any of them
 *
 * @param <T>
 *            The type of the result
 */

public class Promise<T> {

    static {
        // Loading native C++ libraries.
        EmbeddedTools.loadEmbeddedLibraries();
    }

    /**
     * An implementation of this interface can be connected to a {@link Promise}
     * in order to be called when the {@link Promise} receives a cancel request.
     *
     * @param <T>
     *            The type of the result
     */
    public interface CancelRequestCallback<T> {
        void onCancelRequested(Promise<T> promise);
    }

    private long promisePtr;

    private Future<T> future;

    /**
     * Create a new promise
     *
     * @param type
     *            Callback type
     */
    public Promise(FutureCallbackType type) {
        this.promisePtr = this._newPromise(type.nativeValue);
        this.future = new Future<T>(this._getFuture(this.promisePtr));
        this.future.setDefaultFutureCallbackType(type);
    }

    public Promise() {
        this(FutureCallbackType.Async);
    }

    public Future<T> getFuture() {
        return future;
    }

    /**
     * Set the promise value and transfer the information to linked future.<br>
     * Warning after call this method, call {@link #setValue(Object)}, {@link #setError(String)} or {@link #setCancelled()} will cause an IllegalStateException
     * @param value Promise value
     * @throws IllegalStateException If {@link #setValue(Object)}, {@link #setError(String)} or {@link #setCancelled()} was previously called
     */
    public void setValue(T value) {
        _setValue(promisePtr, value);
    }

    /**
     * Set the promise on error state and transfer the information to linked future.<br>
     * Warning after call this method, call {@link #setValue(Object)}, {@link #setError(String)} or {@link #setCancelled()} will cause an IllegalStateException
     * @param errorMessage Error message
     * @throws IllegalStateException If {@link #setValue(Object)}, {@link #setError(String)} or {@link #setCancelled()} was previously called
     */
    public void setError(String errorMessage) {
        // we cannot use a Throwable, libqi must be able to send the errors
        // across
        // the network
        if (errorMessage == null) {
            throw new NullPointerException("errorMessage MUST NOT be null!");
        }

        _setError(promisePtr, errorMessage);
    }

    /**
     * Set the promise on cancel state and transfer the information to linked future.<br>
     * Warning after call this method, call {@link #setValue(Object)}, {@link #setError(String)} or {@link #setCancelled()} will cause an IllegalStateException
     * @throws IllegalStateException If {@link #setValue(Object)}, {@link #setError(String)} or {@link #setCancelled()} was previously called
     */
    public void setCancelled() {
        _setCancelled(promisePtr);
        // the qi Future must match the semantics of
        // java.util.concurrent.Future, so
        // isCancelled() must return true after any successful call to
        // cancel(…); for consistency, any call to promise.setCancelled() must
        // guarantee that any future call to future.isCancelled() also returns
        // true
        // future.setCancelled(); //setCancelled has been deleted
    }

    /**
     * Sets a cancel callback. When cancellation is requested, the set callback
     * is immediately called.
     *
     * @param callback
     *            The callback to call
     */
    public void setOnCancel(CancelRequestCallback<T> callback) {
        _setOnCancel(promisePtr, callback);
    }

    public void connectFromFuture(Future<T> future) {
        future.thenConsume(new Consumer<Future<T>>() {
            @Override
            public void consume(Future<T> future) throws Throwable {
                if (future.isCancelled()) {
                    setCancelled();
                }
                else if (future.hasError()) {
                    setError(future.getError().getMessage());
                }
                else {
                    setValue(future.get());
                }
            }
        });

    }

    /**
     * Called by garbage collector when object destroy.<br>
     * Override to free the reference in JNI.
     *
     * @throws Throwable
     *             On destruction issue.
     */
    @Override
    protected void finalize() throws Throwable {
        this._destroyPromise(this.promisePtr);
        super.finalize();
    }

    private native long _newPromise(int futureCallbackType);

    private native long _getFuture(long promisePtr);

    private native void _setValue(long promisePtr, T value) throws IllegalStateException;

    private native void _setError(long promisePtr, String errorMessage) throws IllegalStateException;

    private native void _setCancelled(long promisePtr) throws IllegalStateException;

    private native void _setOnCancel(long promisePtr, CancelRequestCallback<T> callback);

    /**
     * Destroy the reference in JNI.
     *
     * @param promisePointer
     *            Pointer to destroy.
     */
    private native void _destroyPromise(long promisePointer);
}
