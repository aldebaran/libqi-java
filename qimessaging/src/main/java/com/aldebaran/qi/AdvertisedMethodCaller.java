package com.aldebaran.qi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.aldebaran.qi.serialization.SignatureUtilities;

/**
 * Handle calls to advertised object. See
 * {@link DynamicObjectBuilder#advertiseMethods(Class, Object)}
 *
 * @param <INTERFACE>
 *            Interface type mapped
 */
class AdvertisedMethodCaller<INTERFACE> implements InvocationHandler {
    /** Object builder parent */
    private final DynamicObjectBuilder dynamicObjectBuilder;
    /** Object managed by the builder */
    private AnyObject anyObject;

    /**
     * Create the handler.
     *
     * @param dynamicObjectBuilder
     *            Object builder parent
     */
    AdvertisedMethodCaller(final DynamicObjectBuilder dynamicObjectBuilder) {
        this.dynamicObjectBuilder = dynamicObjectBuilder;
    }

    /**
     * Called when a method is called.
     *
     * @param object
     *            Object instance
     * @param method
     *            Method called
     * @param parameters
     *            Method parameters
     * @return Method result
     */
    @Override
    public Object invoke(final Object object, final Method method, final Object[] parameters) throws Throwable {
        if (this.anyObject == null) {
            this.anyObject = this.dynamicObjectBuilder.object();
        }

        String methodName = method.getName();
        Type returnType = SignatureUtilities.convertNativeTypeToObjectType(method.getGenericReturnType());
        Type[] parametersTypes = SignatureUtilities.convertNativeTypeToObjectType(method.getGenericParameterTypes());
        int length = parameters.length;
        Object[] values = new Object[length];

        for (int i = 0; i < length; i++) {
            values[i] = SignatureUtilities.convertValueJavaToLibQI(parameters[i], parametersTypes[i]);
        }

        return this.anyObject.call(returnType, methodName, values).get();
    }
}
