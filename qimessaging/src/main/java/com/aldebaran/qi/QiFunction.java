package com.aldebaran.qi;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * For convenience, this {@link FutureFunction} allow to implement 3 callbacks
 * independently for result, error or cancellation.
 *
 * This allow to directly get the future value as callback parameter.
 *
 * @param <Ret>
 *          the input future type
 * @param <Arg>
 *          the output future type
 */
public abstract class QiFunction<Ret, Arg> implements FutureFunction<Ret, Arg>
{

  @Override
  public final Future<Ret> execute(Future<Arg> future) throws Throwable
  {
    try
    {
      return onResult(future.get());
    } catch (ExecutionException e)
    {
      return onError(e.getCause());
    } catch (CancellationException e)
    {
      return onCancel();
    }
  }

  public abstract Future<Ret> onResult(Arg result) throws Throwable;

  public Future<Ret> onError(Throwable error) throws Throwable
  {
    throw error;
  }

  public Future<Ret> onCancel() throws Throwable
  {
    return Future.cancelled();
  }
}
