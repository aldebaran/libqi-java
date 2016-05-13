package com.aldebaran.qi;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * Like {@link QiFunction}, this {@link FutureFunction} dispatches 3 callbacks
 * independently for result, error or cancellation.
 *
 * But contrary to {@link QiFunction}, all these methods return void.
 *
 * This is a facility to register a callback on a future, without the need to
 * manage what to return (that will be ignored anyway).
 *
 * @param <T>
 *          the future type
 */
public abstract class QiCallback<T> implements FutureFunction<Void, T>
{
  @Override
  public final Future<Void> execute(Future<T> future) throws Exception
  {
    try
    {
      onResult(future.get());
      return null;
    } catch (ExecutionException e)
    {
      Throwable t = e.getCause();
      onError(t);
      if (t instanceof Exception)
        throw (Exception) t;
      if (t instanceof Error)
        throw (Error) t;
      throw new Error(t);
    } catch (CancellationException e)
    {
      onCancel();
      return Future.cancelled();
    }
  }

  public abstract void onResult(T result) throws Exception;

  public void onError(Throwable error) throws Exception
  {
    // do nothing
  }

  public void onCancel() throws Exception
  {
    // do nothing
  }
}
