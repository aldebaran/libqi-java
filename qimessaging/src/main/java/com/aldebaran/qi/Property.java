package com.aldebaran.qi;

import com.aldebaran.qi.serialization.QiSerializer;

/**
 * Represents a property from distant object or can be advertised
 *
 * @param <T>
 *   Property type. Be sure the type is:
 *    * One of default : Byte, Short, Int, Long, Float, Double, String, List, Map, Tuple, QiStruct.
 *    * Or managed by a {@link QiSerializer}.
 */
public final class Property<T> {

    static {
        // Loading native C++ libraries.
        EmbeddedTools.loadEmbeddedLibraries();
    }

    /**
     * Create a new property instance
     *
     * @return Property pointer
     */
    private static native long createProperty(Class valueClass);

    /**
     * Create a new property with a value.
     * @param valueClass
     * @return
     */
    private static native long createPropertyWithValue(Class valueClass,
                                                       Object value);

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
    private final Class<T> valueClass;

    /** Last set value, to keep the reference alive, don't use it for get */
    private T lastSettedValue;

    /** The serializer used to extract the Property's value */
    private final QiSerializer serializer;

    /**
     * Create a property with a class and a serializer.
     * @param valueClass The property value class. Must not be null.
     * @param serializer Serializer to use for the conversion of the property value. Must not be null.
     */
    public Property(Class<T> valueClass, QiSerializer serializer) {
        Objects.requireNonNull(valueClass, "The class of the property must not be null.");
        Objects.requireNonNull(serializer, "The serializer of the property must not be null.");

        this.valueClass = valueClass;
        this.pointer = createProperty(valueClass);
        this.serializer = serializer;
    }

    /**
     * Create an empty property with a class and the default serializer.
     *
     * @warning The property has an unspecified value until a new one is set.
     * @param valueClass The property value class.
     * @throws NullPointerException if valueClass parameter is null.
     */
    public Property(Class<T> valueClass) {
        this(valueClass, QiSerializer.getDefault());
    }

    /**
     * Create a property with a class, a value and a serializer.
     * @param valueClass The property value class. Must not be null.
     * @param value Value to initialize the property with. Must not be null.
     * @param serializer Serializer to use for the conversion of the property value. Must not be null.
     * @throws a customized NullPointerException if a parameter is null.
     */
    public Property(Class<T> valueClass, T value, QiSerializer serializer) {
        Objects.requireNonNull(valueClass, "The class of the property must not be null.");
        Objects.requireNonNull(value, "The value of the property must not be null.");
        Objects.requireNonNull(serializer, "The serializer of the property must not be null.");

        this.valueClass = valueClass;
        try {
            this.pointer = createPropertyWithValue(valueClass, serializer.serialize(value));
        } catch (QiConversionException e) {
            throw new RuntimeException(e);
        }
        this.serializer = serializer;
    }

    /**
     * Create a property with a value and a serializer.
     *
     * @param value Value to initialize the property with. Must not be null.
     * @param serializer Serializer to use for the conversion of the property value. Must not be null.
     * @throws a customized NullPointerException if a parameter is null.
     */
    public Property(T value, QiSerializer serializer) {
        this(Objects.<Class<T>, Class>uncheckedCast(value.getClass()), value, serializer);
    }

    /**
     * Create a property with a value and the default serializer.
     * @param value Value to initialize the property with. Must not be null
     * @throws a customized NullPointerException if a parameter is null.
     */
    public Property(T value) {
        this(value, QiSerializer.getDefault());
    }

    /**
     * Create a property with a class and a value and the default serializer.
     * @param valueClass The property value class. Must not be null.
     * @param value Value to initialize the property with. Must not be null.
     * @throws a customized NullPointerException if a parameter is null.
     */
    public Property(Class<T> valueClass, T value) {
        this(valueClass, value, QiSerializer.getDefault());
    }

    /**
     * Current serializer.
     * @return
     */
    public QiSerializer getSerializer() { return this.serializer; }

    /**
     * Current property value.
     * It will use the property serializer.
     * For custom type use {@link #getValue(QiSerializer)}
     *
     * @return Future to get the value
     */
    public Future<T> getValue() {
        return this.getValue(this.serializer);
    }

    /**
     * Current property value.
     *
     * @param qiSerializer Serializer to use for deserialize the value. Must not be null
     * @return Future to get the value
     */
    public Future<T> getValue(final QiSerializer qiSerializer) {
        Objects.requireNonNull(serializer, "The serializer of the property getValue must not be null.");

        final Future<Object> future = new Future<Object>(this.get(this.pointer));
        return future.thenApply(new Function<Future<Object>, T>() {
            @Override
            public T execute(Future<Object> future) throws Throwable {
                return (T) qiSerializer.deserialize(future.get(), Property.this.valueClass);
            }
        });
    }

    /**
     * Change property value
     * It will use the property serializer.
     * For custom type use {@link #setValue(QiSerializer, Object)}
     *
     * @param value
     *            New value. Warning may have strange result with {@code null}
     * @return Future to know when value effectively set
     */
    public Future<Void> setValue(final T value) {
        return this.setValue(this.serializer, value);
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
        Objects.requireNonNull(serializer, "The serializer of the property getValue must not be null.");

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
