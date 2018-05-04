package com.aldebaran.qi;

import com.aldebaran.qi.serialization.QiSerializer;

/**
 * Represents a property from distant object or can be advertised
 *
 * @param <T>
 *            Property type. Be sure the type is:
 *            <ul>
 *            <li>One of default : Byte, Short, Int, Long, Float, Double,
 *            String, List, Map, Tuple, QiStruct</li>
 *            <li>Or managed by a {@link QiSerializer}</li>
 *            </ul>
 */
public final class Property<T> {
    /**
     * Create a new property instance
     *
     * @return Property pointer
     */
    private native long createProperty();

    /**
     * Obtain a property value
     *
     * @param pointer
     *            Property pointer
     * @return Pointer on future for get the value
     */
    private native long get(long pointer);

    /**
     * Change property value
     *
     * @param pointer
     *            Property pointer
     * @param value
     *            Property new value
     * @return Pointer on future to know when value effectively set
     */
    private native long set(long pointer, Object value);

    /**
     * Remove property from memory
     *
     * @param pointer
     *            Pointer on property to clean
     */
    private native void destroy(long pointer);

    /** Property pointer reference */
    final long pointer;

    /** Property value type */
    private final Class<T> propertyValueClass;

    /** Last set value, to keep the reference alive, don't use it for get */
    private T lastSettedValue;

    /**
     * Create an empty property.<br>
     * <b>Warning</b> the property have no value until someone set it!
     *
     * @param propertyValueClass
     *            Property value type. Must not be null
     */
    public Property(Class<T> propertyValueClass) {
        if (propertyValueClass == null) {
            throw new NullPointerException("propertyValueClass must not be null");
        }

        this.propertyValueClass = propertyValueClass;
        this.pointer = this.createProperty();
        System.out.println("NONO ->-<- Property # " + this.pointer + " | " + Long.toHexString(this.pointer));
    }

    /**
     * Current property value.<br>
     * Here it used the default serializer, so it suppose that the property type
     * is managed by it.<br>
     * For custom type use {@link #getValue(QiSerializer)}
     *
     * @return Future to get the value
     */
    public Future<T> getValue() {
        return this.getValue(QiSerializer.getDefault());
    }

    /**
     * Current property value.
     *
     * @param qiSerializer
     *            Serializer to use for deserialize the value. Must not be null
     * @return Future to get the value
     */
    public Future<T> getValue(final QiSerializer qiSerializer) {
        if (qiSerializer == null) {
            throw new NullPointerException("qiSerializer must not be null");
        }

        final Future<Object> future = new Future<Object>(this.get(this.pointer));
        return future.thenApply(new Function<Future<Object>, T>() {
            @Override
            public T execute(Future<Object> future) throws Throwable {
                return (T) qiSerializer.deserialize(future.get(), Property.this.propertyValueClass);
            }
        });
    }

    /**
     * Change property value<br>
     * Here it used the default serializer, so it suppose that the property type
     * is managed by it.<br>
     * For custom type use {@link #setValue(QiSerializer, Object)}
     *
     * @param value
     *            New value. Warning may have strange result with {@code null}
     * @return Future to know when value effectively set
     */
    public Future<Void> setValue(final T value) {
        return this.setValue(QiSerializer.getDefault(), value);
    }

    /**
     * Change property value
     *
     * @param qiSerializer
     *            Serializer to use for serialize the value. Must not be null
     * @param value
     *            New value. Warning may have strange result with {@code null}
     * @return Future to know when value effectively set
     */
    public Future<Void> setValue(final QiSerializer qiSerializer, final T value) {
        if (qiSerializer == null) {
            throw new NullPointerException("qiSerializer must not be null");
        }

        try {
            this.lastSettedValue = value;
            this.set(this.pointer, qiSerializer.serialize(this.lastSettedValue));
            return Future.of(null);
        }
        catch (final Throwable throwable) {
            throwable.printStackTrace();
            return Future.fromError(throwable.getMessage());
        }
    }

    /**
     * Called by JVM when object is garbage collected
     *
     * @throws Throwable
     *             On finalization issue
     */
    @Override
    protected void finalize() throws Throwable {
        this.destroy(this.pointer);
        super.finalize();
    }
}