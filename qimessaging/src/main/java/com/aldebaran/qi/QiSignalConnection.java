package com.aldebaran.qi;

public class QiSignalConnection
{
  private AnyObject object;
  private Future<Long> future; // future of native SignalLink

  QiSignalConnection(AnyObject object, Future<Long> future)
  {
    this.object = object;
    this.future = future;
  }

  public Future<Long> getFuture()
  {
    return future;
  }

  public Future<Void> disconnect()
  {
    return object.disconnect(this);
  }

  public void waitForDone()
  {
    future.sync();
  }
}
