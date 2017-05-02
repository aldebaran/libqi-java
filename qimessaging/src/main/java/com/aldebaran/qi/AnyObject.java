/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import com.aldebaran.qi.serialization.QiSerializer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Class that provides type erasure on objects. It represents an object
 * (understandable by the messaging layer) that has shared semantics and that
 * can contain methods, signals and properties.
 * <p>
 * This class is typically used in {@link Session} when retrieving a remote
 * service by its name. The service itself is not retrieved, but a reference to
 * it, in order to call its methods, connect to its signals and access its
 * properties. It is also used when creating and registering a new
 * {@link QiService}.
 * <p>
 * Use {@link DynamicObjectBuilder} to create an instance of this class.
 *
 * @see DynamicObjectBuilder
 */
public class AnyObject {

    static {
        // Loading native C++ libraries.
        if (!EmbeddedTools.LOADED_EMBEDDED_LIBRARY) {
            EmbeddedTools loader = new EmbeddedTools();
            loader.loadEmbeddedLibraries();
        }
    }

    private long _p;

    private native long property(long pObj, String property) throws DynamicCallException;

    private native long setProperty(long pObj, String property, Object value) throws DynamicCallException;

    private native long asyncCall(long pObject, String method, Object[] args) throws DynamicCallException;

    private native String printMetaObject(long pObject);

    private native void destroy(long pObj);

    private native long connect(long pObject, String method, Object instance, String className, String eventName) throws RuntimeException;

    private native long disconnect(long pObject, long subscriberId) throws RuntimeException;

    private native long connectSignal(long pObject, String signalName, QiSignalListener listener);

    private native long disconnectSignal(long pObject, long subscriberId);

    private native long post(long pObject, String name, Object[] args);

    public static native Object decodeJSON(String str);

    public static native String encodeJSON(Object obj);

    /**
     * AnyObject constructor is not public,
     * user must use DynamicObjectBuilder.
     *
     * @see DynamicObjectBuilder
     */
    AnyObject(long p) {
        this._p = p;
    }

    public Future<Void> setProperty(QiSerializer serializer, String property, Object o) {
        try {
            // convert custom structs to tuples if necessary
            Object convertedProperty = serializer.serialize(o);
            return new Future<Void>(setProperty(_p, property, convertedProperty));
        } catch (QiConversionException e) {
            throw new QiRuntimeException(e);
        }
    }

    public Future<Void> setProperty(String property, Object o) {
        return setProperty(QiSerializer.getDefault(), property, o);
    }

    public <T> Future<T> property(String property) {
        return new Future<T>(property(_p, property));
    }

    /**
     * Retrieve the value of {@code property} asynchronously. Tuples will be
     * converted to structs in the result, according to the {@code targetType}.
     *
     * @param targetType the target result type
     * @param property   the property
     * @return a future to the converted result
     */
    public <T> Future<T> getProperty(final QiSerializer serializer, final Type targetType, String property) {
        return property(property).andThen(new QiFunction<T, Object>() {
            @Override
            public Future<T> onResult(Object result) throws Exception {
                @SuppressWarnings("unchecked")
                T convertedResult = (T) serializer.deserialize(result, targetType);
                return Future.of(convertedResult);
            }
        });
    }

    public <T> Future<T> getProperty(Type targetType, String property) {
        return getProperty(QiSerializer.getDefault(), targetType, property);
    }

    public <T> Future<T> getProperty(QiSerializer serializer, Class<T> targetType, String property) {
        // Specialization to benefit from type inference when targetType is a Class
        return getProperty(serializer, (Type) targetType, property);
    }

    public <T> Future<T> getProperty(Class<T> targetType, String property) {
        // Specialization to benefit from type inference when targetType is a Class
        return getProperty((Type) targetType, property);
    }

    /**
     * Perform asynchronous call and return Future return value
     *
     * @param method Method name to call
     * @param args   Arguments to be forward to remote method
     * @return Future method return value
     * @throws DynamicCallException
     */
    public <T> Future<T> call(String method, Object... args) {
        return new Future<T>(asyncCall(_p, method, args));
    }

    /**
     * Convert structs in {@code args} to tuples if necessary, then call
     * {@code method} asynchronously. Tuples will be converted to structs in the
     * result, according to the {@code targetType}.
     *
     * @param targetType the target result type
     * @param method     the method
     * @param args       the method arguments
     * @return a future to the converted result
     */
    public <T> Future<T> call(final QiSerializer serializer, final Type targetType, String method, Object... args) {
        try {
            Object[] convertedArgs = (Object[]) serializer.serialize(args);
            return this.call(method, convertedArgs).andThen(new QiFunction<T, Object>() {
                @Override
                public Future<T> onResult(Object result) throws Exception {
                    @SuppressWarnings("unchecked")
                    T convertedResult = (T) serializer.deserialize(result, targetType);
                    return Future.of(convertedResult);
                }
            });
        } catch (QiConversionException e) {
            throw new QiRuntimeException(e);
        }
    }

    public <T> Future<T> call(final Type targetType, String method, Object... args) {
        return call(QiSerializer.getDefault(), targetType, method, args);
    }

    public <T> Future<T> call(QiSerializer serializer, Class<T> targetType, String method, Object... args) {
        // Specialization to benefit from type inference when targetType is a Class
        return call(serializer, (Type) targetType, method, args);
    }

    public <T> Future<T> call(Class<T> targetType, String method, Object... args) {
        // Specialization to benefit from type inference when targetType is a Class
        return call((Type) targetType, method, args);
    }

    /**
     * Connect a callback to a foreign event.
     *
     * @param eventName Name of the event
     * @param callback  Callback name
     * @param object    Instance of class implementing callback
     * @return an unique subscriber id
     */
    @Deprecated
    public long connect(String eventName, String callback, Object object) {
        Class<? extends Object> c = object.getClass();
        Method[] methods = c.getDeclaredMethods();

        for (Method method : methods) {
            String className = object.getClass().toString();
            className = className.substring(6); // Remove "class "
            className = className.replace('.', '/');

            // If method name match signature
            if (callback.contains(method.getName()) == true)
                return connect(_p, callback, object, className, eventName);
        }

        throw new QiRuntimeException("Cannot find " + callback + " in object " + object);
    }

    public QiSignalConnection connect(String signalName, QiSignalListener listener) {
        long futurePtr = connectSignal(_p, signalName, listener);
        return new QiSignalConnection(this, new Future<Long>(futurePtr));
    }

    public QiSignalConnection connect(final QiSerializer serializer, String signalName, final Object annotatedSlotContainer, String slotName) {
        final Method method = findSlot(annotatedSlotContainer, slotName);

        if (method == null)
            throw new QiSlotException("Slot \"" + slotName + "\" not found in " + annotatedSlotContainer.getClass().getName()
                    + " (did you forget the @QiSlot annotation?)");

        return connect(signalName, new QiSignalListener() {
            @Override
            public void onSignalReceived(Object... args) {
                Object[] convertedArgs = null;
                try {
                    method.setAccessible(true);
                    // convert tuples to custom structs if necessary
                    convertedArgs = serializer.deserialize(args, method.getGenericParameterTypes());
                    method.invoke(annotatedSlotContainer, convertedArgs);
                } catch (IllegalAccessException e) {
                    throw new QiSlotException(e);
                } catch (IllegalArgumentException e) {
                    String message = "Cannot call method " + method + " with parameter types " + Arrays.toString(getTypes(convertedArgs));
                    throw new QiSlotException(message, e);
                } catch (InvocationTargetException e) {
                    throw new QiSlotException(e);
                } catch (QiConversionException e) {
                    throw new QiSlotException(e);
                }
            }
        });
    }

    public QiSignalConnection connect(String signalName, final Object annotatedSlotContainer, String slotName) {
        return connect(QiSerializer.getDefault(), signalName, annotatedSlotContainer, slotName);
    }

    private static Class<?>[] getTypes(Object[] values) {
        Class<?>[] types = new Class[values.length];
        for (int i = 0; i < types.length; ++i) {
            Object value = values[i];
            types[i] = value == null ? null : value.getClass();
        }
        return types;
    }

    Future<Void> disconnect(QiSignalConnection connection) {
        return connection.getFuture().andThen(new QiFunction<Void, Long>() {
            @Override
            public Future<Void> onResult(Long subscriberId) {
                long futurePtr = disconnectSignal(_p, subscriberId);
                return new Future<Void>(futurePtr);
            }
        });
    }

    /**
     * Disconnect a previously registered callback.
     *
     * @param subscriberId id returned by connect()
     */
    @Deprecated
    public long disconnect(long subscriberId) {
        return disconnect(_p, subscriberId);
    }

    /**
     * Post an event advertised with advertiseEvent method.
     *
     * @param eventName Name of the event to trigger.
     * @param args      Arguments sent to callback
     * @see DynamicObjectBuilder#advertiseSignal(long, String)
     */
    public void post(String eventName, Object... args) {
        post(_p, eventName, args);
    }

    @Override
    public String toString() {
        return printMetaObject(_p);
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

    private static Method findSlot(Object annotatedSlotContainer, String slotName) {
        Class<?> clazz = annotatedSlotContainer.getClass();
        Method slot = null;
        for (Method method : clazz.getDeclaredMethods()) {
            QiSlot qiSlot = method.getAnnotation(QiSlot.class);

            if (qiSlot == null)
                // not a slot
                continue;

            String name = qiSlot.value();

            if (name.isEmpty())
                // no name defined in QiSlot, use the method name
                name = method.getName();

            if (slotName.equals(name)) {
                if (slot != null)
                    throw new QiSlotException("More than one slot with name \"" + slotName + "\" in " + clazz.getName());
                slot = method;
                // continue iteration to detect duplicated slot names
            }
        }
        return slot;
    }
}
