package com.aldebaran.qi;

import com.aldebaran.qi.serialization.QiSerializer;

/**
 * Class that represents a connection to a signal. It is retrieved when calling
 * {@link AnyObject#connect(String, QiSignalListener)},
 * {@link AnyObject#connect(QiSerializer, String, Object, String)} or
 * {@link AnyObject#connect(String, Object, String)}.
 *
 * @see AnyObject
 */
public class QiSignalConnection {
    private AnyObject object;
    private Future<Long> future; // future of native SignalLink

    QiSignalConnection(AnyObject object, Future<Long> future) {
        this.object = object;
        this.future = future;
    }

    public Future<Long> getFuture() {
        return future;
    }

    public Future<Void> disconnect() {
        return object.disconnect(this);
    }

    public void waitForDone() {
        future.sync();
    }
}
