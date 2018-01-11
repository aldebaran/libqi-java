/*
 * Copyright (C) 2015 Aldebaran Robotics See COPYING for the license
 */
package com.aldebaran.qi;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Future extends the standard Java {@link java.util.concurrent.Future} and
 * represents the result of an asynchronous computation.
 * <p>
 * {@link Promise} and Future are two complementary concepts. They are designed
 * to synchronize data between multiples threads. The Future holds the result of
 * an asynchronous computation, from which you can retrieve the value of the
 * result; the {@link Promise} sets the value of this computation, which
 * resolves the associated Future.
 *
 * @param <T>
 *            The type of the result
 */
public class Future<T> implements java.util.concurrent.Future<T> {
    /**
     * Consumer caller to report an error
     */
    static class ReportBugConsumer<T1> implements Consumer<Future<T1>> {
        /**
         * Called when future is finished
         */
        @Override
        public void consume(Future<T1> future) throws Throwable {
            if (future.hasError() && !future.continuationSpecified.get()) {
                System.err.println("Issue on Future: " + future.getErrorMessage());
                future.getError().printStackTrace();
            }
        }
    }

    // Loading QiMessaging JNI layer
    static {
        if (!EmbeddedTools.LOADED_EMBEDDED_LIBRARY) {
            EmbeddedTools loader = new EmbeddedTools();
            loader.loadEmbeddedLibraries();
        }
    }

    public interface Callback<T> {
        void onFinished(Future<T> future);
    }

    private static final int TIMEOUT_INFINITE = -1;

    // C++ Future
    private final long _fut;

    // Native C API object functions
    private native boolean qiFutureCallCancel(long pFuture);

    private native boolean qiFutureCallCancelRequest(long pFuture);

    private native Object qiFutureCallGet(long pFuture, int msecs) throws ExecutionException, TimeoutException;

    private native boolean qiFutureCallIsCancelled(long pFuture);

    private native boolean qiFutureCallIsDone(long pFuture);

    private native void qiFutureCallWaitWithTimeout(long pFuture, int timeout);

    private native void qiFutureDestroy(long pFuture);

    private native void qiFutureCallConnectCallback(long pFuture, Callback<?> callback, int futureCallbackType);

    /**
     * Call native future "andThen" for Function(P)->R
     *
     * @param future
     *            Future pointer
     * @param function
     *            Function to callback
     * @return Pointer of created future
     */
    private native long qiFutureAndThen(long future, Function<T, ?> function);

    /**
     * Call native future "andThen" for Consumer(P)
     *
     * @param future
     *            Future pointer
     * @param consumer
     *            Function to callback
     * @return Pointer of created future
     */
    private native long qiFutureAndThenVoid(long future, Consumer<T> consumer);

    /**
     * Call native future "andThen" for Function(P)->Future&lt;R&gt; and unwrap
     * automatically
     *
     * @param future
     *            Future pointer
     * @param futureOfFutureFunction
     *            Function to callback
     * @return Pointer of created future
     */
    private native long qiFutureAndThenUnwrap(long future, Function<T, ?> futureOfFutureFunction);

    /**
     * Call native future <b>then</b> for Function(Future&lt;P&gt;)->R
     *
     * @param future
     *            Future pointer
     * @param function
     *            Function to callback
     * @return Pointer on created future
     */
    private native long qiFutureThen(long future, Function<Future<T>, ?> futureFunction);

    /**
     * Call native future <b>then</b> for Consumer(Future&lt;P&gt;)
     *
     * @param future
     *            Future pointer
     * @param function
     *            Function to callback
     * @return Pointer on created future
     */
    private native long qiFutureThenVoid(long future, Consumer<Future<T>> futureFunction);

    /**
     * Call native future <b>then</b> for
     * Function(Future&lt;P&gt;)->Future&lt;R&gt; and unwrap automatically
     *
     * @param future
     *            Future pointer
     * @param function
     *            Function to callback
     * @return Pointer on created future
     */
    private native long qiFutureThenUnwrap(long future, Function<Future<T>, ?> futureOfFutureFunction);

    /**
     * Default callback type
     */
    private FutureCallbackType defaultFutureCallbackType = FutureCallbackType.Async;

    /**
     * Indicates if at least one continuation is specified, that is to say that
     * {@link #thenApply(Function)}, {@link #thenCompose(Function)},
     * {@link #thenConsume(Consumer)}, {@link #andThenApply(Function)},
     * {@link #andThenCompose(Function)} or {@link #andThenConsume(Consumer)}
     * was called
     */
    final AtomicBoolean continuationSpecified;

    Future(final long pFuture) {
        this._fut = pFuture;
        this.continuationSpecified = new AtomicBoolean(false);
        this.qiFutureThenVoid(this._fut, new ReportBugConsumer<T>());
    }

    /**
     * Change the default callback type
     *
     * @param defaultFutureCallbackType
     *            New default callback type
     */
    void setDefaultFutureCallbackType(FutureCallbackType defaultFutureCallbackType) {
        this.defaultFutureCallbackType = defaultFutureCallbackType;
    }

    public static <T> Future<T> of(final T value) {
        Promise<T> promise = new Promise<T>();
        promise.setValue(value);
        return promise.getFuture();
    }

    public static <T> Future<T> cancelled() {
        Promise<T> promise = new Promise();
        promise.setCancelled();
        return promise.getFuture();
    }

    public static <T> Future<T> fromError(String errorMessage) {
        Promise<T> promise = new Promise<T>();
        promise.setError(errorMessage);
        return promise.getFuture();
    }

    public void sync(long timeout, TimeUnit unit) {
        qiFutureCallWaitWithTimeout(_fut, (int) unit.toMillis(timeout));
    }

    public void sync() {
        this.sync(0, TimeUnit.SECONDS);
    }

    /**
     * Prefer {@link #then(FutureFunction, FutureCallbackType)} instead (e.g.
     */
    public void connect(Callback<T> callback, FutureCallbackType futureCallbackType) {
        qiFutureCallConnectCallback(_fut, callback, futureCallbackType.nativeValue);
    }

    public void connect(final Callback<T> callback) {
        this.connect(callback, this.defaultFutureCallbackType);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // ignore mayInterruptIfRunning, can't map it to native libqi
        // This must be a blocking call to be compliant with Java's Future
        qiFutureCallCancel(_fut);
        sync();
        return isCancelled();
    }

    /**
     * Sends asynchronously a request to cancel the execution of this task.
     */
    public synchronized void requestCancellation() {
        // This call is compliant with native libqi's Future.cancel()
        qiFutureCallCancelRequest(_fut);
    }

    @Deprecated
    public synchronized boolean cancel() {
        // Leave this method as it is (even if returning a boolean doesn't make
        // sense)
        // to avoid breaking projects that were already using it.
        // Future projects should prefer using requestCancellation().
        return qiFutureCallCancel(_fut);
    }

    @SuppressWarnings("unchecked")
    private T get(int msecs) throws ExecutionException, TimeoutException {
        try {
            return (T) qiFutureCallGet(_fut, msecs);
        }
        catch (Exception exception) {
            Throwable throwable = exception;

            while (throwable != null) {
                if (throwable instanceof CancellationException) {
                    throw (CancellationException) throwable;
                }

                if (throwable instanceof TimeoutException) {
                    throw (TimeoutException) throwable;
                }

                if (throwable instanceof QiException) {
                    throwable = NativeTools.obtainRealException((QiException) throwable);

                    if (throwable instanceof QiException) {
                        throw (QiException) throwable;
                    }

                    continue;
                }

                throwable = throwable.getCause();
            }

            Exception newException = NativeTools.obtainRealException(exception);
            throw new ExecutionException(newException.getMessage(), newException);
        }
    }

    @Override
    public T get() throws ExecutionException {
        try {
            return get(TIMEOUT_INFINITE);
        }
        catch (TimeoutException e) {
            // should never happen
            throw new RuntimeException(e);
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException {
        int msecs = (int) unit.toMillis(timeout);
        return get(msecs);
    }

    /**
     * Same as {@code get()}, but does not throw checked exceptions.
     * <p>
     * This is especially useful for getting the value of a future that we know
     * is complete with success.
     *
     * @return the future value
     */
    public T getValue() {
        try {
            return get();
        }
        catch (ExecutionException e) {
            // this is an error to call getValue() if the future is not finished
            // with a value
            throw new RuntimeException(e);
        }
    }

    public ExecutionException getError() {
        try {
            get();
            return null;
        }
        catch (ExecutionException e) {
            return e;
        }
        catch (CancellationException e) {
            return null;
        }
    }

    public boolean hasError() {
        return getError() != null;
    }

    public String getErrorMessage() {
        // for convenience
        ExecutionException e = getError();
        if (e == null) {
            return null;
        }

        Throwable throwable = e;
        String message = null;

        while (throwable != null) {

            if (message == null) {
                message = throwable.getMessage();
            }

            if (throwable instanceof QiException) {
                return ((QiException) throwable).getMessage();
            }

            throwable = throwable.getCause();
        }

        if (message == null) {
            message = e.toString();
        }

        return message;
    }

    @Override
    public synchronized boolean isCancelled() {
        // inherited from java.util.concurrent.Future, it must match its
        // semantics
        // i.e. it must return true after any successful call to cancel(…)
        // --> There is no way to verify that a cancel request resulted in the
        // cancellation of the associated task, until the Future is done
        return qiFutureCallIsCancelled(_fut);
    }

    @Override
    public synchronized boolean isDone() {
        return qiFutureCallIsDone(_fut);
    }

    public synchronized boolean isSuccess() {
        return !this.isCancelled() && !this.hasError();
    }

    /**
     * Launch a task when the task link when this future finished (succeed,
     * error or cancelled)
     *
     * @param <R>
     *            Function result type
     * @param function
     *            Function to call when action the task link when this future
     *            finished
     * @return Future for able to link to the end of the global execution (the
     *         task link to this future and given function) status
     */
    public <R> Future<R> thenApply(final Function<Future<T>, R> function) {
        this.continuationSpecified.set(true);
        final long futurePointer = this.qiFutureThen(this._fut, function);
        return new Future<R>(futurePointer);
    }

    /**
     * Launch a task when the task link when this future finished (succeed,
     * error or cancelled)
     *
     * @param consumer
     *            Consumer to call when action the task link when this future
     *            finished
     * @return Future for able to link to the end of the global execution (the
     *         task link to this future and given ) status
     */
    public Future<Void> thenConsume(final Consumer<Future<T>> consumer) {
        this.continuationSpecified.set(true);
        final long futurePointer = this.qiFutureThenVoid(this._fut, consumer);
        return new Future<Void>(futurePointer);
    }

    /**
     * Launch a task when the task link when this future finished (succeed,
     * error or cancelled).<br>
     * Instead of return a Future<Future<R>>, it will automatically unwrap the
     * result to have a Future&lt;R&gt;
     *
     * @param <R>
     *            Function result type
     * @param function
     *            Function to call when action the task link when this future
     *            finished
     * @return Future for able to link to the end of the global execution (the
     *         task link to this future and given ) status
     */
    public <R> Future<R> thenCompose(final Function<Future<T>, Future<R>> function) {
        this.continuationSpecified.set(true);
        final long pointer = this.qiFutureThenUnwrap(this._fut, function);
        return new Future<R>(pointer);
    }

    /**
     * Launch a task when the task link when this future succeed only
     *
     * @param <R>
     *            Function result type
     * @param function
     *            Function to call when action the task link when this future
     *            succeed only
     * @return Future for able to link to the end of the global execution (the
     *         task link to this future and given function) status
     */
    public <R> Future<R> andThenApply(final Function<T, R> function) {
        this.continuationSpecified.set(true);
        final long futurePointer = this.qiFutureAndThen(this._fut, function);
        return new Future<R>(futurePointer);
    }

    /**
     * Launch a task when the task link when this future succeed only
     *
     * @param consumer
     *            Consumer to call when action the task link when this future
     *            succeed only
     * @return Future for able to link to the end of the global execution (the
     *         task link to this future and given ) status
     */
    public Future<Void> andThenConsume(final Consumer<T> consumer) {
        this.continuationSpecified.set(true);
        final long futurePointer = this.qiFutureAndThenVoid(this._fut, consumer);
        return new Future<Void>(futurePointer);
    }

    /**
     * Launch a task when the task link when this future succeed only.<br>
     * Instead of return a Future<Future<R>>, it will automatically unwrap the
     * result to have a Future&lt;R&gt;
     *
     * @param <R>
     *            Function result type
     * @param function
     *            Function to call when action the task link when this future
     *            succeed only
     * @return Future for able to link to the end of the global execution (the
     *         task link to this future and given ) status
     */
    public <R> Future<R> andThenCompose(final Function<T, Future<R>> function) {
        this.continuationSpecified.set(true);
        final long pointer = this.qiFutureAndThenUnwrap(this._fut, function);
        return new Future<R>(pointer);
    }

    /**
     * Wait for all {@code futures} to complete.
     * <p>
     * The returning future finishes successfully if and only if all of the
     * futures it waits for finish successfully.
     * <p>
     * Otherwise, it takes the state of the first failing future (due to
     * cancellation or error).
     *
     * @param futures
     *            the futures to wait for
     * @return a future waiting for all the others
     */
    public static Future<Void> waitAll(final Future<?>... futures) {
        if (futures.length == 0)
            return Future.of(null);

        class WaitData {
            int runningFutures = futures.length;
            boolean stopped;
        }
        final WaitData waitData = new WaitData();
        final Promise<Void> promise = new Promise<Void>();
        promise.setOnCancel(new Promise.CancelRequestCallback<Void>() {
            @Override
            public void onCancelRequested(Promise<Void> promise) {
                for (Future<?> future : futures) {
                    future.requestCancellation();
                }
            }
        });

        for (Future<?> future : futures) {
            ((Future<Object>) future).connect(new Future.Callback<Object>() {
                @Override
                public void onFinished(Future<Object> future) {
                    synchronized (waitData) {
                        if (!waitData.stopped) {
                            if (future.isCancelled()) {
                                promise.setCancelled();
                                waitData.stopped = true;
                            }
                            else if (future.hasError()) {
                                promise.setError(future.getErrorMessage());
                                waitData.stopped = true;
                            }
                            else {
                                waitData.runningFutures--;

                                if (waitData.runningFutures == 0) {
                                    promise.setValue(null);
                                }
                            }
                        }
                    }
                }
            });
        }

        return promise.getFuture();
    }

    /**
     * Return a version of {@code this} future that waits until {@code futures}
     * to finish.
     * <p>
     * The returning future finishes successfully if and only if {@code this}
     * future and {@code futures} finish successfully.
     * <p>
     * Otherwise, it takes the state of {@code this} if it failed, or the first
     * failing future from {@code futures}.
     * <p>
     * If {@code this} future does not finish successfully, it does not wait for
     * {@code futures}.
     *
     * @param futures
     *            the futures to wait for
     * @return future returning this future value when all {@code futures} are
     *         finished successfully
     */
    public Future<T> waitFor(final Future<?>... futures) {
        return this.andThenCompose(new Function<T, Future<T>>() {
            @Override
            public Future<T> execute(final T value) throws Throwable {
                return Future.waitAll(futures).andThenApply(new Function<Void, T>() {
                    @Override
                    public T execute(Void ignored) throws Throwable {
                        return value;
                    }

                });
            }
        });
    }

    /**
     * Called by garbage collector Finalize is overridden to manually delete C++
     * data
     */
    @Override
    protected void finalize() throws Throwable {
        this.qiFutureDestroy(this._fut);
        super.finalize();
    }
}
