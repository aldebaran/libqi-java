package com.aldebaran.qimessaging;

import java.lang.reflect.Method;

public class Object {

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

  private static native long     property(long pObj, String property);
  private static native long     setProperty(long pObj, String property, java.lang.Object value);
  private static native long     asyncCall(long pObject, String method, java.lang.Object[] args);
  private static native String   printMetaObject(long pObject);
  private static native void     destroy(long pObj);
  private static native long     connect(long pObject, String method, java.lang.Object instance, String className, String eventName);
  private static native long     post(long pObject, String name, java.lang.Object[] args);

  public static native java.lang.Object decodeJSON(String str);
  public static native String encodeJSON(java.lang.Object obj);

  /**
   * Object constructor is not public,
   * user must use DynamicObjectBuilder.
   * @see DynamicObjectBuilder
   */
  Object(long p)
  {
    this._p = p;
  }

  public Future<Void> setProperty(String property, java.lang.Object o) throws Exception
  {
    return new Future<Void>(Object.setProperty(_p, property, o));
  }

  public <T> Future<T> property(String property)
  {
    return new Future<T>(Object.property(_p, property));
  }

  /**
   * Perform asynchronous call and return Future return value
   * @param method Method name to call
   * @param args Arguments to be forward to remote method
   * @return Future method return value
   * @throws CallError
   */
  public <T> Future<T> call(String method, java.lang.Object ... args) throws CallError
  {
    com.aldebaran.qimessaging.Future<T> ret = null;

    try
    {
      ret = new com.aldebaran.qimessaging.Future<T>(Object.asyncCall(_p, method, args));
    } catch (Exception e)
    {
      throw new CallError(e.getMessage());
    }

    if (ret.isValid() == false)
      throw new CallError("Future is null.");

    try
    {
      return ret;
    } catch (Exception e)
    {
      throw new CallError(e.getMessage());
    }
  }

  /**
   * Connect a callback to a foreign event.
   * @param eventName Name of the event
   * @param callback Callback name
   * @param object Instance of class implementing callback
   * @throws Exception If callback method is not found in object instance.
   */
  public long connect(String eventName, String callback, java.lang.Object object) throws Exception
  {
    Class<?extends java.lang.Object> c = object.getClass();
    Method[] methods = c.getDeclaredMethods();

    for (Method method : methods)
    {
      String className = object.getClass().toString();
      className = className.substring(6); // Remove "class "
      className = className.replace('.', '/');

      // If method name match signature
      if (callback.contains(method.getName()) == true)
        return Object.connect(_p, callback, object, className, eventName);
    }

    throw new Exception("Cannot find " + callback + " in object " + object.toString());
  }

  /**
   * Post an event advertised with advertiseEvent method.
   * @see advertiseEvent
   * @param eventName Name of the event to trigger.
   * @param args Arguments sent to callback
   */
  public void post(String eventName, java.lang.Object ... args)
  {
    Object.post(_p, eventName, args);
  }

  public String toString()
  {
    return Object.printMetaObject(_p);
  }

  /**
   * Called by garbage collector
   * Finalize is overriden to manually delete C++ data
   */
  @Override
  protected void finalize() throws Throwable
  {
    Object.destroy(_p);
    super.finalize();
  }
}
