package com.aldebaran.qi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Monitor the call of concrete instance's methods.<br>
 * It collects the exception that may happen while execute a method
 *
 * @param <INTERFACE>
 *            Mapped interface
 */
class AdvertisedMethodMonitor<INTERFACE> implements InvocationHandler {
    /** Concrete instance */
    private final INTERFACE instance;
    /** Last collected exception */
    private Exception exception;

    /***
     * Create the monitor
     *
     * @param instance
     *            Concrete instance
     */
    AdvertisedMethodMonitor(INTERFACE instance) {
        this.instance = instance;
    }

    /**
     * Last collected exception
     *
     * @return Last collected exception
     */
    Exception getException() {
        return this.exception;
    }

    /**
     * Called when a method is invoked
     *
     * @param proxy
     *            Managed object
     * @param method
     *            Method to invoke
     * @param parameters
     *            Method parameters
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] parameters) throws Throwable {
        try {
            return method.invoke(this.instance, parameters);
        }
        catch (InvocationTargetException exception) {
            // We are interested by the cause, because we want hide the
            // reflection/proxy part and obtain the real exception
            this.exception = (Exception) exception.getCause();
            throw exception;
        }
        catch (Exception exception) {
            this.exception = exception;
            throw exception;
        }
    }

}
