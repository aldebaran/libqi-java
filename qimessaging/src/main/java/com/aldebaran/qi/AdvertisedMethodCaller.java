package com.aldebaran.qi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.aldebaran.qi.serialization.QiSerializer;
import com.aldebaran.qi.serialization.SignatureUtilities;

/**
 * Handle calls to advertised object. See
 * {@link DynamicObjectBuilder#advertiseMethods(Class, Object)}
 *
 * @param <INTERFACE> Interface type mapped
 */
class AdvertisedMethodCaller<INTERFACE> implements InvocationHandler {
    /**
     * Object builder parent
     */
    private final DynamicObjectBuilder dynamicObjectBuilder;
    /**
     * Object managed by the builder
     */
    private AnyObject anyObject;
    /**
     * Monitor manager to get last exception
     */
    private final AdvertisedMethodMonitor<INTERFACE> advertisedMethodMonitor;
    /**
     * Serializer to use
     */
    private final QiSerializer serializer;

    /**
     * Create the handler.
     *
     * @param serializer
     *            Serializer to use
     * @param dynamicObjectBuilder
     *            Object builder parent
     * @param advertisedMethodMonitor
     *            Monitor manager to get last exception
     */
    AdvertisedMethodCaller(final QiSerializer serializer, final DynamicObjectBuilder dynamicObjectBuilder,
            final AdvertisedMethodMonitor<INTERFACE> advertisedMethodMonitor) {
        this.serializer = serializer;
        this.dynamicObjectBuilder = dynamicObjectBuilder;
        this.advertisedMethodMonitor = advertisedMethodMonitor;
    }

    /**
     * Called when a method is called.
     *
     * @param object     Object instance
     * @param method     Method called
     * @param parameters Method parameters
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
        int length = 0;

        if(parameters!=null){
            length = parameters.length;
        }

        Object[] values = new Object[length];

        for (int i = 0; i < length; i++) {
            values[i] = SignatureUtilities.convertValueJavaToLibQI(parameters[i], parametersTypes[i]);
        }

        synchronized (this.anyObject) {
            try {
                Object value = this.anyObject.call(this.serializer, returnType, methodName, values).get();

                if (value != null && !method.getReturnType().equals(value.getClass())) {
                    System.err.println("Different type between expected result type and value type. Libqi does not wrap the good class ... method.getReturnType()="
                            + method.getReturnType().getName() + " | value.getClass()=" + value.getClass().getName());

                    if (SignatureUtilities.isDouble(method.getReturnType()) && SignatureUtilities.isNumber(value.getClass())) {
                        value = new Double(((Number) value).doubleValue());
                    }
                }

                return value;
            }
            catch (Exception exception) {
                // Get the last exception
                Exception exception2 = this.advertisedMethodMonitor.getException();

                if (exception2 != null) {
                    exception = exception2;
                }

                throw exception;
            }
        }
    }
}
