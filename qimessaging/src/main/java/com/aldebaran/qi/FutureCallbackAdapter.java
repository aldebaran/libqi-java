package com.aldebaran.qi;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import com.aldebaran.qi.Future.Callback;

public abstract class FutureCallbackAdapter<T> implements Callback<T>
{
  @Override
  public final void onFinished(Future<T> future)
  {
    try
    {
      onResult(future.get());
    } catch (ExecutionException e)
    {
      onError(e.getCause());
    } catch (CancellationException e)
    {
      onCancel();
    }
  }

  public abstract void onResult(T result);

  public void onError(Throwable error)
  {
    // do nothing
  }

  public void onCancel()
  {
    // do nothing
  }
}
