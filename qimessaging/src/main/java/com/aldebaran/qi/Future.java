/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author proullon
 *
 * @param <T>
 */
public class Future <T>
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

  // C++ Future
  private long  _fut;

  // Native C API object functions
  private static native boolean qiFutureCallCancel(long pFuture);
  private static native Object  qiFutureCallGet(long pFuture);
  private static native Object  qiFutureCallGetWithTimeout(long pFuture, int timeout);
  private static native String  qiFutureCallGetError(long pFuture);
  private static native boolean qiFutureCallIsCancelled(long pFuture);
  private static native boolean qiFutureCallIsDone(long pFuture);
  private static native boolean qiFutureCallConnect(long pFuture, Object callback, String className, Object[] args);
  private static native void    qiFutureCallWaitWithTimeout(long pFuture, int timeout);
  private static native void    qiFutureDestroy(long pFuture);

  private Future()
  {

  }

  Future(long pFuture)
  {
    _fut = pFuture;
  }

  public void sync(long timeout, TimeUnit unit)
  {
    Future.qiFutureCallWaitWithTimeout(_fut, (int) unit.toMillis(timeout));
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
  public boolean addCallback(Callback<?> callback, Object ... args)
  {
    String className = callback.getClass().toString();
    className = className.substring(6); // Remove "class "
    className = className.replace('.', '/');

    return qiFutureCallConnect(_fut, callback, className, args);
  }

  public boolean cancel()
  {

    return qiFutureCallCancel(_fut);
  }

  @SuppressWarnings("unchecked")
  public T get() throws InterruptedException
  {

    Object ret = null;

    try
    {
      ret = Future.qiFutureCallGet(_fut);
    } catch (Exception e)
    {
      throw new RuntimeException(e.getMessage());
    }

    if (isCancelled())
      throw new InterruptedException();

    return (T) ret;
  }

  @SuppressWarnings("unchecked")
  public T get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException
  {

    Object ret = null;
    int timeoutms = (int) unit.toMillis(timeout);

    try
    {
      ret = Future.qiFutureCallGetWithTimeout(_fut, timeoutms);
    } catch (Exception e)
    {
      throw new RuntimeException(e.getMessage());
    }

    if (ret == null)
      throw new TimeoutException();

    return (T) ret;
  }

  public String getError()
  {
    return Future.qiFutureCallGetError(_fut);
  }

  public boolean isCancelled()
  {
    return Future.qiFutureCallIsCancelled(_fut);
  }

  public boolean isDone()
  {
    return Future.qiFutureCallIsDone(_fut);
  }

  public boolean isValid()
  {
    if (_fut == 0)
      return false;

    return true;
  }

  /**
   * Called by garbage collector
   * Finalize is overriden to manually delete C++ data
   */
  @Override
  protected void finalize() throws Throwable
  {
    Future.qiFutureDestroy(_fut);
    super.finalize();
  }

}
