/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import java.lang.reflect.Method;

/**
 * Class that exposes directly an {@link AnyObject} that can be manipulated.
 * <p>
 * There is only one {@link AnyObject} per DynamicObjectBuilder, the object
 * method always returns the same instance.
 * <p>
 * This class is typically used to subscribe a new {@link QiService} to a
 * {@link Session}.
 */

public class DynamicObjectBuilder {

    static {
        // Loading native C++ libraries.
        if (!EmbeddedTools.LOADED_EMBEDDED_LIBRARY) {
            EmbeddedTools loader = new EmbeddedTools();
            loader.loadEmbeddedLibraries();
        }
    }

    private long _p;

    private native long create();

    private native void destroy(long pObject);

    private native AnyObject object(long pObjectBuilder);

    private native void advertiseMethod(long pObjectBuilder, String method, Object instance, String className, String description);

    private native void advertiseSignal(long pObjectBuilder, String eventSignature);

    private native void advertiseProperty(long pObjectBuilder, String name, Class<?> propertyBase);

    private native void setThreadSafeness(long pObjectBuilder, boolean isThreadSafe);

    /// Possible thread models for an object

    /**
     * Enum to declare the thread-safeness state of an {@link AnyObject} instance.
     * <p>
     * Use <b>MultiThread</b> if your object is expected to be thread-safe.
     * Method calls will potentially occur in parallel in multiple threads.
     * <p>
     * Use <b>SingleThread</b> to make your object non-thread-safe.
     * All method calls must occur in the same thread.
     */
    public enum ObjectThreadingModel {
        /// AnyObject is not thread safe, all method calls must occur in the same thread
        SingleThread,
        /// AnyObject is thread safe, multiple calls can occur in different threads in parallel
        MultiThread
    }

    public DynamicObjectBuilder() {
        _p = create();
    }

  /**
   * Bind method from a qimessaging.service to GenericObject.
   * @param methodSignature Signature of method to bind.
   * @param service Service implementing method.
   * @throws Exception on error.
   */
  public void advertiseMethod(String methodSignature, QiService service, String description) {
    final Class<? extends QiService> serviceClass = service.getClass();
    final Method[] methods = serviceClass.getDeclaredMethods();
    final String serviceClassName = serviceClass.getName().replace('.', '/');

    for (final Method method : methods) {
      // FIXME this is very fragile
      // If method name match signature
      if (methodSignature.contains(method.getName())) {
        advertiseMethod(_p, methodSignature, service, serviceClassName, description);
        return;
      }
    }
  }

    /**
     * Advertise a signal with its callback signature.
     *
     * @param signalSignature Signature of available callback.
     * @throws Exception If GenericObject is not initialized internally.
     */
    public void advertiseSignal(String signalSignature) throws Exception {
        advertiseSignal(_p, signalSignature);
    }

    public void advertiseProperty(String name, Class<?> propertyBase) {
        advertiseProperty(_p, name, propertyBase);
    }

    /**
     * Declare the thread-safeness state of an instance
     *
     * @param threadModel if set to ObjectThreadingModel.MultiThread,
     *                    your object is expected to be
     *                    thread safe, and calls to its method will potentially
     *                    occur in parallel in multiple threads.
     *                    If false, qimessaging will use a per-instance mutex
     *                    to prevent multiple calls at the same time.
     */
    public void setThreadingModel(ObjectThreadingModel threadModel) {
        setThreadSafeness(_p, threadModel == ObjectThreadingModel.MultiThread);
    }

    /**
     * Instantiate new AnyObject after builder template.
     *
     * @return AnyObject
     * @see AnyObject
     */
    public AnyObject object() {
        return object(_p);
    }

    /**
     * Called by garbage collector
     * Finalize is overriden to manually delete C++ data
     */
    @Override
    protected void finalize() throws Throwable {
        destroy(_p);
        super.finalize();
    }
}
