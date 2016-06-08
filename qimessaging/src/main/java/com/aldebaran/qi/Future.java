/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author proullon
 *
 * @param <T>
 */
public class Future<T> implements java.util.concurrent.Future<T>
{

  // Loading QiMessaging JNI layer
  static
  {
    if (!EmbeddedTools.LOADED_EMBEDDED_LIBRARY)
    {
      EmbeddedTools loader = new EmbeddedTools();
      loader.loadEmbeddedLibraries();
    }
  }

  public interface Callback<T>
  {
    void onFinished(Future<T> future);
  }

  private static final int TIMEOUT_INFINITE = -1;

  // C++ Future
  private long  _fut;

  // Native C API object functions
  private native boolean qiFutureCallCancel(long pFuture);
  private native Object qiFutureCallGet(long pFuture, int msecs) throws ExecutionException, TimeoutException;
  private native boolean qiFutureCallIsCancelled(long pFuture);
  private native boolean qiFutureCallIsDone(long pFuture);
  private native boolean qiFutureCallConnect(long pFuture, Object callback, String className, Object[] args);
  private native void qiFutureCallWaitWithTimeout(long pFuture, int timeout);
  private native void qiFutureDestroy(long pFuture);
  private native void qiFutureCallConnectCallback(long pFuture, Callback<?> callback, int futureCallbackType);
  private static native long qiFutureCreate(Object value);

  private boolean cancelled;

  Future(long pFuture)
  {
    _fut = pFuture;
  }

  public static <T> Future<T> of(T value)
  {
    return new Future<T>(qiFutureCreate(value));
  }

  public static <T> Future<T> cancelled()
  {
    Promise<T> promise = new Promise<T>();
    promise.setCancelled();
    return promise.getFuture();
  }

  public static <T> Future<T> fromError(String errorMessage)
  {
    Promise<T> promise = new Promise<T>();
    promise.setError(errorMessage);
    return promise.getFuture();
  }

  public void sync(long timeout, TimeUnit unit)
  {
    qiFutureCallWaitWithTimeout(_fut, (int) unit.toMillis(timeout));
  }

  public void sync()
  {
    this.sync(0, TimeUnit.SECONDS);
  }

  /**
   * Callbacks to future can be set.
   * @param callback com.aldebaran.qi.Callback implementation
   * @param args Argument to be forwarded to callback functions.
   * @return true on success.
   * @since 1.20
   */
  @Deprecated
  public boolean addCallback(com.aldebaran.qi.Callback<?> callback, Object ... args)
  {
    String className = callback.getClass().toString();
    className = className.substring(6); // Remove "class "
    className = className.replace('.', '/');

    return qiFutureCallConnect(_fut, callback, className, args);
  }

  /**
   * Prefer {@link #then(FutureFunction, FutureCallbackType)} instead (e.g. {@link QiCallback}).
   */
  public void connect(Callback<T> callback, FutureCallbackType futureCallbackType)
  {
    qiFutureCallConnectCallback(_fut, callback, futureCallbackType.nativeValue);
  }

  public void connect(Callback<T> callback)
  {
    connect(callback, FutureCallbackType.Auto);
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning)
  {
    // ignore mayInterruptIfRunning, can't map it to native libqi
    return cancel();
  }

  public synchronized boolean cancel()
  {
    if (qiFutureCallCancel(_fut))
      cancelled = true;
    return cancelled;
  }

  /* package */ synchronized void setCancelled() {
    // to be called from Promise
    cancelled = true;
  }

  @SuppressWarnings("unchecked")
  private T get(int msecs) throws ExecutionException, TimeoutException
  {

    // inherited from java.util.concurrent.Future, it must match its semantics
    // i.e. it must throw after any successful call to cancel(…)
    if (isCancelled())
      throw new CancellationException();

    // there is a harmless race condition here: if cancel() is called after the
    // check but before the call to qiFutureCallGet(), then the cancellation
    // will be handled only after get() has returned

    Object result = qiFutureCallGet(_fut, msecs);

    // we don't want to keep the "cancelled" mutex during qiFutureCallGet()
    // so we have to check again
    if (isCancelled())
      throw new CancellationException();

    return (T) result;
  }

  @Override
  public T get() throws ExecutionException
  {
    try
    {
      return get(TIMEOUT_INFINITE);
    } catch (TimeoutException e)
    {
      // should never happen
      throw new RuntimeException(e);
    }
  }

  @Override
  public T get(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException
  {
    int msecs = (int) unit.toMillis(timeout);
    return get(msecs);
  }

  /**
   * Same as {@code get()}, but does not throw checked exceptions.
   *
   * This is especially useful for getting the value of a future that we know is
   * complete with success.
   *
   * @return the future value
   */
  public T getValue()
  {
    try
    {
      return get();
    } catch (ExecutionException e)
    {
      // this is an error to call getValue() if the future is not finished with a value
      throw new RuntimeException(e);
    }
  }

  public ExecutionException getError()
  {
    try
    {
      get();
      return null;
    } catch (ExecutionException e)
    {
      return e;
    } catch (CancellationException e) {
      return null;
    }
  }

  public boolean hasError()
  {
    return getError() != null;
  }

  public String getErrorMessage()
  {
    // for convenience
    ExecutionException e = getError();
    if (e == null) {
      return null;
    }
    Throwable cause = e.getCause();
    if (cause instanceof QiException) {
      return ((QiException) cause).getMessage();
    }
    return e.getMessage();
  }

  @Override
  public synchronized boolean isCancelled()
  {
    // inherited from java.util.concurrent.Future, it must match its semantics
    // i.e. it must return true after any successful call to cancel(…)
    return cancelled;
  }

  @Override
  public synchronized boolean isDone()
  {
    // inherited from java.util.concurrent.Future, it must match its semantics
    // i.e. it must return true after any successful call to cancel(…)
    if (cancelled)
      return true;
    return qiFutureCallIsDone(_fut);
  }

  private <Ret> Future<Ret> _then(final FutureFunction<Ret, T> function, final boolean chainOnFailure,
      FutureCallbackType type)
  {
    // the promise must be sync according to the Callback (which may be Sync or Async according to the caller)
    final Promise<Ret> promiseToNotify = new Promise<Ret>(FutureCallbackType.Sync);
    connect(new Callback<T>()
    {
      @Override
      public void onFinished(Future<T> future)
      {
        if (chainOnFailure || !notifyIfFailed(future, promiseToNotify))
          chainFuture(future, function, promiseToNotify);
      }
    }, type);
    return promiseToNotify.getFuture();
  }

  public <Ret> Future<Ret> then(FutureFunction<Ret, T> function, FutureCallbackType type)
  {
    return _then(function, true, type);
  }

  public <Ret> Future<Ret> then(FutureFunction<Ret, T> function)
  {
    return _then(function, true, FutureCallbackType.Auto);
  }

  public <Ret> Future<Ret> andThen(FutureFunction<Ret, T> function, FutureCallbackType type)
  {
    return _then(function, false, type);
  }

  public <Ret> Future<Ret> andThen(FutureFunction<Ret, T> function)
  {
    return _then(function, false, FutureCallbackType.Auto);
  }

  private static <Ret, Arg> Future<Ret> getNextFuture(Future<Arg> future, FutureFunction<Ret, Arg> function)
  {
    try {
      Future<Ret> result = function.execute(future);
      // for convenience, the function can return null, which means a future with a null value
      if (result == null)
        result = Future.of(null);
      return result;
    } catch (Throwable t) {
      // print the trace because the future error is a string, so the stack trace is lost
      t.printStackTrace();
      return fromError(t.toString());
    }
  }

  private static <T> void notifyPromiseFromFuture(Future<T> future, Promise<T> promiseToNotify)
  {
    FutureResult<T> result = future.getResult();
    switch (result.type)
    {
    case FutureResult.TYPE_ERROR:
      promiseToNotify.setError(result.errorMessage);
      break;
    case FutureResult.TYPE_CANCELLED:
      promiseToNotify.setCancelled();
      break;
    case FutureResult.TYPE_VALUE:
      promiseToNotify.setValue(result.value);
      break;
    default:
      throw new UnsupportedOperationException("Unsupported result type");
    }
  }

  private static <Ret, Arg> void chainFuture(Future<Arg> future, FutureFunction<Ret, Arg> function,
      final Promise<Ret> promiseToNotify)
  {
    getNextFuture(future, function).connect(new Callback<Ret>()
    {
      @Override
      public void onFinished(Future<Ret> future)
      {
        notifyPromiseFromFuture(future, promiseToNotify);
      }
    });
  }

  private static <T> boolean notifyIfFailed(Future<T> future, Promise<?> promiseToNotify)
  {
    FutureResult<T> result = future.getResult();
    switch (result.type)
    {
    case FutureResult.TYPE_ERROR:
      promiseToNotify.setError(result.errorMessage);
      return true;
    case FutureResult.TYPE_CANCELLED:
      promiseToNotify.setCancelled();
      return true;
    }
    return false;
  }

  /**
   * Wait for all {@code futures} to complete.
   *
   * The returning future finishes successfully if and only if all of the
   * futures it waits for finish successfully.
   *
   * Otherwise, it takes the state of the first failing future (due to
   * cancellation or error).
   *
   * @param futures
   *          the futures to wait for
   * @return a future waiting for all the others
   */
  public static Future<Void> waitAll(final Future<?>... futures)
  {
    if (futures.length == 0)
      return Future.of(null);

    class WaitData
    {
      int runningFutures = futures.length;
      boolean stopped;
    }
    final WaitData waitData = new WaitData();
    final Promise<Void> promise = new Promise<Void>();
    for (Future<?> future : futures)
    {
      @SuppressWarnings("unchecked")
      Future<Object> objectFuture = (Future<Object>) future;
      objectFuture.connect(new Future.Callback<Object>()
      {
        @Override
        public void onFinished(Future<Object> future)
        {
          FutureResult<Object> result = future.getResult();
          synchronized (waitData)
          {
            if (!waitData.stopped)
            {
              switch (result.type)
              {
              case FutureResult.TYPE_CANCELLED:
                promise.setCancelled();
                waitData.stopped = true;
                break;
              case FutureResult.TYPE_ERROR:
                promise.setError(result.errorMessage);
                waitData.stopped = true;
                break;
              case FutureResult.TYPE_VALUE:
                if (--waitData.runningFutures == 0)
                  promise.setValue(null);
                break;
              default:
                throw new UnsupportedOperationException("Unsupported result type");
              }
            }
          }
        }
      });
    }
    return promise.getFuture();
  }

  /**
   * Return a version of {@code this} future that waits until {@code futures} to
   * finish.
   *
   * The returning future finishes successfully if and only if {@code this}
   * future and {@code futures} finish successfully.
   *
   * Otherwise, it takes the state of {@code this} if it failed, or the first
   * failing future from {@code futures}.
   *
   * If {@code this} future does not finish successfully, it does not wait for
   * {@code futures}.
   *
   * @param futures
   *          the futures to wait for
   * @return future returning this future value when all {@code futures} are
   *         finished successfully
   */
  public Future<T> waitFor(final Future<?>... futures)
  {
    // do not wait for futures if this does not finish successfully
    return andThen(new FutureFunction<T, T>()
    {
      @Override
      public Future<T> execute(Future<T> future)
      {
        return waitAll(futures).andThen(new FutureFunction<T, Void>()
        {
          @Override
          public Future<T> execute(Future<Void> future)
          {
            return Future.this;
          }
        });
      }
    });
  }

  private synchronized FutureResult<T> getResult() {
    assert isDone(); // do not check at runtime, it is private
    if (hasError())
      return FutureResult.error(getErrorMessage());
    if (isCancelled())
      return FutureResult.cancelled();
    return FutureResult.value(getValue());
  }

  /**
   * Called by garbage collector
   * Finalize is overriden to manually delete C++ data
   */
  @Override
  protected void finalize() throws Throwable
  {
    qiFutureDestroy(_fut);
    super.finalize();
  }

  private static class FutureResult<T>
  {
    static final int TYPE_VALUE = 0;
    static final int TYPE_ERROR = 1;
    static final int TYPE_CANCELLED = 2;
    int type;
    T value;
    String errorMessage;

    private FutureResult(int type, T value, String errorMessage)
    {
      this.type = type;
      this.value = value;
      this.errorMessage = errorMessage;
    }

    static <T> FutureResult<T> value(T value)
    {
      return new FutureResult<T>(TYPE_VALUE, value, null);
    }

    static <T> FutureResult<T> error(String errorMessage)
    {
      return new FutureResult<T>(TYPE_ERROR, null, errorMessage);
    }

    static <T> FutureResult<T> cancelled()
    {
      return new FutureResult<T>(TYPE_CANCELLED, null, null);
    }
  }
}
