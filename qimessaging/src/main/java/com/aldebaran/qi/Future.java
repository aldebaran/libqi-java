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
  private native long qiFutureCallThen(long pFuture, QiFunction<?, ?> function);
  private native long qiFutureCallAndThen(long pFuture, QiFunction<?, ?> function);
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
    return e == null ? null : e.getMessage();
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

  public <Ret> Future<Ret> then(QiFunction<Ret, T> function)
  {
    return new Future<Ret>(qiFutureCallThen(_fut, function));
  }

  public <Ret> Future<Ret> andThen(QiFunction<Ret, T> function)
  {
    return new Future<Ret>(qiFutureCallAndThen(_fut, function));
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
