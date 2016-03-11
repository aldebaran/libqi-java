/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import com.aldebaran.qi.ClientAuthenticatorFactory;

public class Session
{

  static
  {
    // Loading native C++ libraries.
    if (!EmbeddedTools.LOADED_EMBEDDED_LIBRARY)
    {
      EmbeddedTools loader = new EmbeddedTools();
      loader.loadEmbeddedLibraries();
    }
  }

  // Native function
  private native long qiSessionCreate();
  private native void qiSessionDestroy(long pSession);
  private native long qiSessionConnect(long pSession, String url);
  private native boolean qiSessionIsConnected(long pSession);
  private native void qiSessionClose(long pSession);
  private native long service(long pSession, String name);
  private native int registerService(long pSession, String name, AnyObject obj);
  private native void unregisterService(long pSession, int idx);
  private native void onDisconnected(long pSession, String callback, Object obj);
  private native void setClientAuthenticatorFactory(long pSession, ClientAuthenticatorFactory factory);

  // Members
  private long _session;
  private boolean _destroy;

  /**
   * Create a qimessaging session.
   */
  public Session()
  {
    _session = qiSessionCreate();
    _destroy = true;
  }

  protected Session(long session)
  {
    _session = session;
    _destroy = false;
  }

  /**
   * @return true is session is connected, false otherwise
   */
  public boolean isConnected()
  {
    if (_session == 0)
      return false;

    return qiSessionIsConnected(_session);
  }

  /**
   * Try to connect to given address.
   * @param serviceDirectoryAddress Address to connect to.
   * @throws Exception on error.
   */
  public Future<Void> connect(String serviceDirectoryAddress)
  {
    long pFuture = qiSessionConnect(_session, serviceDirectoryAddress);
    return new Future<Void>(pFuture);
  }

  /**
   * Ask for remote service to Service Directory.
   * @param name Name of service.
   * @return the AnyObject future
   */
  public Future<AnyObject> service(String name)
  {
    long pFuture = service(_session, name);
    return new Future<AnyObject>(pFuture);
  }

  /**
   * Close connection to Service Directory
   */
  public void close()
  {
    qiSessionClose(_session);
  }

  /**
   * Called by garbage collector
   * Finalize is overriden to manually delete C++ data
   */
  @Override
  protected void finalize() throws Throwable
  {
    //if (_destroy)
    //  Session.qiSessionDestroy(_session);
    super.finalize();
  }

  /**
   * Register service on Service Directory
   * @param name Name of new service
   * @param object Instance of service
   * @return
   */
  public int registerService(String name, AnyObject object)
  {
    return registerService(_session, name, object);
  }

  /**
   * Unregister service from Service Directory
   * @param idx is return by registerService
   * @see registerService
   */
  public void unregisterService(int idx)
  {
    unregisterService(_session, idx);
  }

  public void onDisconnected(String callback, Object object)
  {
    onDisconnected(_session, callback, object);
  }

  public void setClientAuthenticatorFactory(ClientAuthenticatorFactory factory)
  {
    setClientAuthenticatorFactory(_session, factory);
  }
}
