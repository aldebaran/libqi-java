package com.aldebaran.qi;

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
  private static native long    qiSessionCreate();
  private static native void    qiSessionDestroy(long pSession);
  private static native long    qiSessionConnect(long pSession, String url);
  private static native boolean qiSessionIsConnected(long pSession);
  private static native void    qiSessionClose(long pSession);
  private static native AnyObject  service(long pSession, String name);
  private static native int     registerService(long pSession, String name, AnyObject obj);
  private static native void    unregisterService(long pSession, int idx);
  private static native void    onDisconnected(long pSession, String callback, Object obj);

  // Members
  private long _session;
  private boolean _destroy;

  /**
   * Create session and try to connect to given address.
   * @param sdAddr Address to connect to.
   * @throws Exception on error.
   */
  public Session(String sdAddr) throws Exception
  {
    _session = Session.qiSessionCreate();
    this.connect(sdAddr).sync();
    _destroy = true;
  }

  /**
   * Create a qimessaging session.
   */
  public Session()
  {
    _session = Session.qiSessionCreate();
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

    return Session.qiSessionIsConnected(_session);
  }

  /**
   * Try to connect to given address.
   * @param serviceDirectoryAddress Address to connect to.
   * @throws Exception on error.
   */
  public Future<Void> connect(String serviceDirectoryAddress) throws Exception
  {
    long pFuture = Session.qiSessionConnect(_session, serviceDirectoryAddress);
    com.aldebaran.qi.Future<Void> future = new com.aldebaran.qi.Future<Void>(pFuture);
    return future;
  }

  /**
   * Ask for remote service to Service Directory.
   * @param name Name of service.
   * @return Proxy on remote service on success, null on error.
   */
  public AnyObject  service(String name) throws Exception
  {
    return (AnyObject) Session.service(_session, name);
  }

  /**
   * Close connection to Service Directory
   */
  public void 	close()
  {
    Session.qiSessionClose(_session);
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
    return Session.registerService(_session, name, object);
  }

  /**
   * Unregister service from Service Directory
   * @param idx is return by registerService
   * @see registerService
   */
  public void unregisterService(int idx)
  {
    Session.unregisterService(_session, idx);
  }

  public void onDisconnected(String callback, Object object)
  {
    Session.onDisconnected(_session, callback, object);
  }
}
