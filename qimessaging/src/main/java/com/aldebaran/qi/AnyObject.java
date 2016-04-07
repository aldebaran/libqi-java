/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import java.lang.reflect.Method;

public class AnyObject {

  static
  {
    // Loading native C++ libraries.
    if (!EmbeddedTools.LOADED_EMBEDDED_LIBRARY)
    {
      EmbeddedTools loader = new EmbeddedTools();
      loader.loadEmbeddedLibraries();
    }
  }

  private long    _p;

  private native long property(long pObj, String property);
  private native long setProperty(long pObj, String property, Object value);
  private native long asyncCall(long pObject, String method, Object[] args);
  private native String printMetaObject(long pObject);
  private native void destroy(long pObj);
  private native long connect(long pObject, String method, Object instance, String className, String eventName);
  private native long disconnect(long pObject, long subscriberId);
  private native long connectSignal(long pObject, String signalName, QiSignalListener listener);
  private native long disconnectSignal(long pObject, long subscriberId);
  private native long post(long pObject, String name, Object[] args);

  public static native Object decodeJSON(String str);
  public static native String encodeJSON(Object obj);

  /**
   * AnyObject constructor is not public,
   * user must use DynamicObjectBuilder.
   * @see DynamicObjectBuilder
   */
  AnyObject(long p)
  {
    this._p = p;
  }

  public Future<Void> setProperty(String property, Object o)
  {
    return new Future<Void>(setProperty(_p, property, o));
  }

  public <T> Future<T> property(String property)
  {
    return new Future<T>(property(_p, property));
  }

  /**
   * Perform asynchronous call and return Future return value
   * @param method Method name to call
   * @param args Arguments to be forward to remote method
   * @return Future method return value
   * @throws DynamicCallException
   */
  public <T> Future<T> call(String method, Object... args)
  {
    return new Future<T>(asyncCall(_p, method, args));
  }

  /**
   * Connect a callback to a foreign event.
   * @param eventName Name of the event
   * @param callback Callback name
   * @param object Instance of class implementing callback
   * @return an unique subscriber id
   */
  public long connect(String eventName, String callback, Object object)
  {
    Class<?extends Object> c = object.getClass();
    Method[] methods = c.getDeclaredMethods();

    for (Method method : methods)
    {
      String className = object.getClass().toString();
      className = className.substring(6); // Remove "class "
      className = className.replace('.', '/');

      // If method name match signature
      if (callback.contains(method.getName()) == true)
        return connect(_p, callback, object, className, eventName);
    }

    throw new QiRuntimeException("Cannot find " + callback + " in object " + object);
  }

  public QiSignalConnection connect(String signalName, QiSignalListener listener)
  {
    long futurePtr = connectSignal(_p, signalName, listener);
    return new QiSignalConnection(this, new Future<Long>(futurePtr));
  }

  Future<Void> disconnect(QiSignalConnection connection)
  {
    return connection.getFuture().andThen(new QiFunctionAdapter<Void, Long>()
    {
      @Override
      public Future<Void> handleResult(Long subscriberId)
      {
        long futurePtr = disconnectSignal(_p, subscriberId);
        return new Future<Void>(futurePtr);
      }
    });
  }

  /**
   * Disconnect a previously registered callback.
   * @param subscriberId id returned by connect()
   *
   */
  public long disconnect(long subscriberId)
  {
    return disconnect(_p, subscriberId);
  }

  /**
   * Post an event advertised with advertiseEvent method.
   * @see advertiseEvent
   * @param eventName Name of the event to trigger.
   * @param args Arguments sent to callback
   */
  public void post(String eventName, Object ... args)
  {
    post(_p, eventName, args);
  }

  @Override
  public String toString()
  {
    return printMetaObject(_p);
  }

  /**
   * Called by garbage collector
   * Finalize is overriden to manually delete C++ data
   */
  @Override
  protected void finalize() throws Throwable
  {
    destroy(_p);
    super.finalize();
  }
}
