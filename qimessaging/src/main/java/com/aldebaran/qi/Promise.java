package com.aldebaran.qi;

/**
 * Promise is a writable, single assignment container which sets the value of the {@link Future}.
 *
 * Promise and {@link Future} are two complementary concepts. They are designed to synchronise data
 * between multiples threads. The {@link Future} holds the result of an asynchronous computation,
 * from which you can retrieve the value of the result; the Promise sets the value of this computation,
 * which resolves the associated {@link Future}.
 *
 * For promises that don't need a value and are just used to ensure correct ordering of asynchronous
 * operations, the common pattern to use is {@link java.lang.Void} as a generic type.
 *
 * @param <T> The type of the result
 */

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

  public Promise(FutureCallbackType type)
  {
    promisePtr = _newPromise(type.nativeValue);
    future = new Future<T>(_getFuture(promisePtr));
  }

  public Promise()
  {
    this(FutureCallbackType.Auto);
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

  public void connectFromFuture(Future<T> future)
  {
    future.then(new QiCallback<T>()
    {

      @Override
      public void onResult(T result) throws Exception
      {
        setValue(result);
      }

      @Override
      public void onError(Throwable error) throws Exception
      {
        setError(error.getMessage());
      }

      @Override
      public void onCancel() throws Exception
      {
        setCancelled();
      }
    });
  }

  private native long _newPromise(int futureCallbackType);

  private native long _getFuture(long promisePtr);

  private native void _setValue(long promisePtr, T value);

  private native void _setError(long promisePtr, String errorMessage);

  private native void _setCancelled(long promisePtr);
}
