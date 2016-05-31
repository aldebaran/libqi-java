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
  private native void qiFutureCallConnectCallback(long pFuture, Callback<?> callback);
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
   * Prefer {@link #then(FutureFunction)} instead (e.g. {@link QiCallback}).
   */
  public void connect(Callback<T> callback)
  {
    qiFutureCallConnectCallback(_fut, callback);
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

  private <Ret> Future<Ret> _then(final FutureFunction<Ret, T> function, final boolean chainOnFailure)
  {
    final Promise<Ret> promiseToNotify = new Promise<Ret>();
    connect(new Callback<T>()
    {
      @Override
      public void onFinished(Future<T> future)
      {
        if (chainOnFailure || !notifyIfFailed(future, promiseToNotify))
          chainFuture(future, function, promiseToNotify);
      }
    });
    return promiseToNotify.getFuture();
  }

  public <Ret> Future<Ret> then(FutureFunction<Ret, T> function)
  {
    return _then(function, true);
  }

  public <Ret> Future<Ret> andThen(FutureFunction<Ret, T> function)
  {
    return _then(function, false);
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
    // we must lock the mutex, because the future can be cancelled at any time
    synchronized (future)
    {
      if (future.hasError())
        promiseToNotify.setError(future.getErrorMessage());
      else if (future.isCancelled())
        promiseToNotify.setCancelled();
      else
        promiseToNotify.setValue(future.getValue());
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
    synchronized (future)
    {
      if (future.hasError())
      {
        promiseToNotify.setError(future.getErrorMessage());
        return true;
      }
      if (future.isCancelled())
      {
        promiseToNotify.setCancelled();
        return true;
      }
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
          synchronized (waitData)
          {
            if (!waitData.stopped)
            {
              if (future.isCancelled())
              {
                promise.setCancelled();
                waitData.stopped = true;
              } else if (future.hasError())
              {
                promise.setError(future.getErrorMessage());
                waitData.stopped = true;
              } else if (--waitData.runningFutures == 0)
                promise.setValue(null);
            }
          }
        }
      });
    }
    return promise.getFuture();
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

}
