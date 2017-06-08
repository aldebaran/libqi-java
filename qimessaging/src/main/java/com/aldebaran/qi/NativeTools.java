package com.aldebaran.qi;

import java.lang.reflect.Method;

import com.aldebaran.qi.serialization.MethodDescription;
import com.aldebaran.qi.serialization.SignatureUtilities;

/**
 * Utilities tools to communicate with native code (Code in C++)
 */
public class NativeTools {
    /**
     * Call a Java method (Generally called from JNI)
     *
     * @param instance
     *            Instance on which the method is called.
     * @param methodName
     *            Method name to call.
     * @param javaSignature
     *            Method Java signature.
     * @param arguments
     *            Method parameters.
     * @return Method result.
     */
    public static Object callJava(final Object instance, final String methodName, final String javaSignature,
            final Object[] arguments) {
        if (instance != null) {
            try {
                MethodDescription methodDescription = MethodDescription.fromJNI(methodName, javaSignature);
                Class<?> claz = instance.getClass();
                Method method = null;
                int distance = Integer.MAX_VALUE;
                int dist;

                for (Method meth : claz.getMethods()) {
                    dist = methodDescription.distance(meth);

                    if (dist < distance) {
                        distance = dist;
                        method = meth;
                    }
                }

                if (method != null) {
                    Class<?>[] parametersTarget = method.getParameterTypes();

                    for (int index = arguments.length - 1; index >= 0; index--) {
                        arguments[index] = SignatureUtilities.convert(arguments[index], parametersTarget[index]);
                    }

                    Object result = method.invoke(instance, arguments);
                    Class<?> returnType = methodDescription.getReturnType();

                    if (void.class.equals(returnType) || Void.class.equals(returnType))
                        return null;

                    return SignatureUtilities.convertValueJavaToLibQI(result, methodDescription.getReturnType());
                }
            }
            catch (Exception exception) {
                exception.printStackTrace();
                throw new RuntimeException(NativeTools.computeMessage(exception), exception);
            }
        }

        return null;
    }

    /**
     * Compute complete message of exception or error
     *
     * @param throwable
     *            Exception or error
     * @return Complete message
     */
    public static String computeMessage(Throwable throwable) {
        final StringBuilder stringBuilder = new StringBuilder();
        boolean first = true;

        while (throwable != null) {
            if (!first) {
                stringBuilder.append(" (caused by) ");
            }

            stringBuilder.append(throwable.toString());
            first = false;
            throwable = throwable.getCause();
        }

        return stringBuilder.toString();
    }
}
