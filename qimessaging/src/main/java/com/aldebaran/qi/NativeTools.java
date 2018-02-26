package com.aldebaran.qi;

import com.aldebaran.qi.serialization.MethodDescription;
import com.aldebaran.qi.serialization.SignatureUtilities;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utilities tools to communicate with native code (Code in C++)
 */
public class NativeTools {
    /**
     * Header of error message key
     */
    private static final String ERROR_MESSAGE_HEADER = "$ERROR_MESSAGE_NativeTools_";
    /**
     * Footer of error message key
     */
    private static final String ERROR_MESSAGE_FOOTER = "$";
    /**
     * Next message error ID
     */
    private static final AtomicLong NEXT_EXCEPTION_ID = new AtomicLong(0);
    /**
     * Map of error messages stored to be get later
     */
    private static final Map<String, Exception> ERRORS_MAP = new HashMap<String, Exception>();

    /**
     * Get the real exception corresponding to given one.<br>
     * If the given exception have special message, we get our stored exception,
     * else return the exception itself
     *
     * @param exception Exception to get its real version
     * @return Real exception
     */
    static Exception obtainRealException(Exception exception) {
        // Test if its a managed exception
        String message = exception.toString();
        int start = message.indexOf(ERROR_MESSAGE_HEADER);

        if (start < 0) {
            return exception;
        }

        int end = message.indexOf(ERROR_MESSAGE_FOOTER, start + ERROR_MESSAGE_HEADER.length());

        if (end < start) {
            return exception;
        }

        // /It is a managed exception, extract the key and get the associated
        // exception
        String key = message.substring(start, end + ERROR_MESSAGE_FOOTER.length());
        Exception realException = ERRORS_MAP.get(key);

        if (realException == null) {
            return exception;
        }

        return realException;
    }

    /**
     * Store an exception and return a replace one to use
     *
     * @param exception Exception to store
     * @return Exception to use
     */
    private static RuntimeException storeException(Exception exception) {
        String message = ERROR_MESSAGE_HEADER + NEXT_EXCEPTION_ID.getAndIncrement() + ERROR_MESSAGE_FOOTER;
        ERRORS_MAP.put(message, exception);
        return new RuntimeException(message, exception);
    }

    /**
     * Call a Java method (Generally called from JNI)
     *
     * @param instance      Instance on which the method is called.
     * @param methodName    Method name to call.
     * @param javaSignature Method Java signature.
     * @param arguments     Method parameters.
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

                    method.setAccessible(true);
                    Object result = method.invoke(instance, arguments);
                    Class<?> returnType = methodDescription.getReturnType();

                    if (void.class.equals(returnType) || Void.class.equals(returnType))
                        return null;

                    return SignatureUtilities.convertValueJavaToLibQI(result, methodDescription.getReturnType());
                }
            } catch (InvocationTargetException invocationTargetException) {
                // We are interested by the cause, because we want hide the
                // reflection/proxy part and obtain the real exception
                throw storeException((Exception) invocationTargetException.getCause());
            } catch (Exception exception) {
                throw storeException(exception);
            }
        }

        return null;
    }
}
