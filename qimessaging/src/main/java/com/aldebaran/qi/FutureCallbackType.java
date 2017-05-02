package com.aldebaran.qi;

/**
 * Enum to specify how callbacks registered to a {@link Future} are called.
 * Callbacks can be invoked synchronously or asynchronously using a thread from
 * an internal thread pool, by setting the corresponding value:
 * {@link FutureCallbackType#Sync} or {@link FutureCallbackType#Async}, in the
 * {@link Promise} constructor.
 */
public enum FutureCallbackType {
    // keep values synchronized with qi::FutureCallbackType in libqi/qi/detail/future_fwd.hpp
    Sync(0), Async(1), Auto(2);

    int nativeValue;

    private FutureCallbackType(int nativeValue) {
        this.nativeValue = nativeValue;
    }
}
