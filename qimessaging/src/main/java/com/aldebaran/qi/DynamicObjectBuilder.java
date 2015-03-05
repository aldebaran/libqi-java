/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import java.lang.reflect.Method;

public class DynamicObjectBuilder {

  static
  {
    // Loading native C++ libraries.
    if (!EmbeddedTools.LOADED_EMBEDDED_LIBRARY)
    {
      EmbeddedTools loader = new EmbeddedTools();
      loader.loadEmbeddedLibraries();
    }
  }

  private long _p;

  private static native long   create();
  private static native void   destroy(long pObject);
  private static native Object object(long pObjectBuilder);
  private static native long   advertiseMethod(long pObjectBuilder, String method, Object instance, String className, String description);
  private static native long   advertiseSignal(long pObjectBuilder, String eventSignature);
  private static native long   advertiseProperty(long pObjectBuilder, String name, Class<?> propertyBase);
  private static native long   advertiseThreadSafeness(long pObjectBuilder, boolean isThreadSafe);

  /// Possible thread models for an object
  public enum ObjectThreadingModel
  {
    /// AnyObject is not thread safe, all method calls must occur in the same thread
    SingleThread,
    /// AnyObject is thread safe, multiple calls can occur in different threads in parallel
    MultiThread
  }

  public DynamicObjectBuilder()
  {
    _p = DynamicObjectBuilder.create();
  }

  /**
   * Bind method from a qimessaging.service to GenericObject.
   * @param methodSignature Signature of method to bind.
   * @param service Service implementing method.
   * @throws Exception on error.
   */
  public void advertiseMethod(String methodSignature, QiService service, String description) throws QiException
  {
    Class<?extends Object> c = service.getClass();
    Method[] methods = c.getDeclaredMethods();

    if (_p == 0)
      throw new QiException("Invalid object.\n");

    for (Method method : methods)
    {
      String className = service.getClass().toString();
      className = className.substring(6); // Remove "class "
      className = className.replace('.', '/');

      // If method name match signature
      if (methodSignature.contains(method.getName()) == true)
      {
        if (DynamicObjectBuilder.advertiseMethod(_p, methodSignature, service, className, description) == 0)
          throw new QiException("Cannot register method " + methodSignature);
        return;
      }
    }
  }

  /**
   * Advertise an signal with its callback signature.
   * @param signalSignature Signature of available callback.
   * @throws Exception If GenericObject is not initialized internally.
   */
  public void advertiseSignal(String signalSignature) throws Exception
  {
    if (_p == 0)
      throw new Exception("Invalid object");
    DynamicObjectBuilder.advertiseSignal(_p, signalSignature);
  }

  public void advertiseProperty(String name, Class<?> propertyBase) throws QiException
  {
    if (_p == 0)
      throw new QiException("Invalid object");
    if (DynamicObjectBuilder.advertiseProperty(_p, name, propertyBase) <= 0)
      throw new QiException("Cannot advertise " + name + " property");
  }

  /**
   * Declare the thread-safeness state of an instance
   * @param threadModel if set to ObjectThreadingModel.MultiThread,
   *        your object is expected to be
   *        thread safe, and calls to its method will potentially
   *        occur in parallel in multiple threads.
   *        If false, qimessaging will use a per-instance mutex
   *        to prevent multiple calls at the same time.
   */
  public void setThreadingModel(ObjectThreadingModel threadModel) throws QiException
  {
    if (_p == 0)
      throw new QiException("Invalid object");
    DynamicObjectBuilder.advertiseThreadSafeness(_p, threadModel == ObjectThreadingModel.MultiThread);
  }

  /**
   * Instantiate new AnyObject after builder template.
   * @see AnyObject
   * @return AnyObject
   */
  public AnyObject object()
  {
    return (AnyObject) DynamicObjectBuilder.object(_p);
  }

  /**
   * Called by garbage collector
   * Finalize is overriden to manually delete C++ data
   */
  @Override
  protected void finalize() throws Throwable
  {
    DynamicObjectBuilder.destroy(_p);
    super.finalize();
  }
}
