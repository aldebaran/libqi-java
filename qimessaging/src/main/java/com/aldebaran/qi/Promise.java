package com.aldebaran.qi;

public class Promise<T>
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

  private long promisePtr;

  private Future<T> future;

  public Promise()
  {
    promisePtr = _newPromise();
    future = new Future<T>(_getFuture(promisePtr));
  }

  public Future<T> getFuture()
  {
    return future;
  }

  public void setValue(T value)
  {
    _setValue(promisePtr, value);
  }

  public void setError(String errorMessage)
  {
    // we cannot use a Throwable, libqi must be able to send the errors across
    // the network
    _setError(promisePtr, errorMessage);
  }

  public void setCancelled()
  {
    _setCancelled(promisePtr);
    // the qi Future must match the semantics of java.util.concurrent.Future, so
    // isCancelled() must return true after any successful call to
    // cancel(…); for consistency, any call to promise.setCancelled() must
    // guarantee that any future call to future.isCancelled() also returns true
    future.setCancelled();
  }

  private native long _newPromise();

  private native long _getFuture(long promisePtr);

  private native void _setValue(long promisePtr, T value);

  private native void _setError(long promisePtr, String errorMessage);

  private native void _setCancelled(long promisePtr);
}
