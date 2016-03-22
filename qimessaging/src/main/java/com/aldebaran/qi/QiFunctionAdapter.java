package com.aldebaran.qi;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * For convenience, this {@link QiFunction} allow to implement 3 callbacks
 * independently for result, error or cancellation.
 *
 * This allow to directly get the future value as callback parameter.
 *
 * @param <Ret>
 *          the input future type
 * @param <Arg>
 *          the output future type
 */
public abstract class QiFunctionAdapter<Ret, Arg> implements QiFunction<Ret, Arg>
{

  @Override
  public final Future<Ret> execute(Future<Arg> future) throws Exception
  {
    try
    {
      return handleResult(future.get());
    } catch (ExecutionException e)
    {
      return handleError(e);
    } catch (CancellationException e)
    {
      return handleCancellation(e);
    }
  }

  public abstract Future<Ret> handleResult(Arg result) throws Exception;

  public Future<Ret> handleError(ExecutionException exception) throws Exception
  {
    throw exception;
  }

  public Future<Ret> handleCancellation(CancellationException exception) throws Exception
  {
    throw exception;
  }

}
