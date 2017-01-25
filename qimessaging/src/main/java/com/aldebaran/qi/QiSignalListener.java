package com.aldebaran.qi;

/**
 * An implementation of this interface can be set as a callback to be invoked
 * every time the specified signal is triggered.
 *
 * @see QiSignalConnection
 * @see AnyObject
 */
public interface QiSignalListener {
    void onSignalReceived(Object... args);
}
