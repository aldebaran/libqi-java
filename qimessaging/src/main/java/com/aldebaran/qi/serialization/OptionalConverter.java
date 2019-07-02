package com.aldebaran.qi.serialization;

import com.aldebaran.qi.Function;
import com.aldebaran.qi.Optional;
import com.aldebaran.qi.QiConversionException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Converter for {@link Optional} objects.
 * <p>
 * This class is responsible for adapting {@link Optional} objects of a custom
 * value type from and to {@link Optional} objects of a type that the native
 * layer can handle.
 *
 * @see QiSerializer
 * @see QiSerializer.Converter
 * @see Optional
 * @since 3.1.0
 */
public class OptionalConverter implements QiSerializer.Converter {
    /**
     * @return True if {@code object} is an instance of {@link Optional}.
     */
    @Override
    public boolean canSerialize(Object object) {
        return object instanceof Optional;
    }

    /**
     * Creates an optional object adapted from an optional object {@code
     * object}. The logic is:
     * <ul>
     * <li>If the optional has a value, recursively adapts the
     * value by calling {@link QiSerializer#serialize(Object)} on it.</li>
     * <li>Otherwise, just returns an empty generic {@link Optional}.</li>
     * </ul>
     *
     * @throws QiConversionException
     *         <ul>
     *         <li>If {@code !canSerialize(object)}, then the
     *         cause of the exception is an {@link IllegalArgumentException}.</li>
     *         <li>If the serialization of the value throws an exception.</li>
     *         </ul>
     * @see #canSerialize(Object)
     * @see Optional
     * @see Optional#map(Function)
     */
    @Override
    public Optional<?> serialize(final QiSerializer serializer, Object object)
            throws QiConversionException {
        try {
            if (!canSerialize(object)) {
                throw new IllegalArgumentException(
                        "The object cannot be serialized by this.");
            }

            Optional<?> opt = (Optional<?>) object;
            return opt.map(new Function<Object, Object>() {
                @Override
                public Object execute(Object value) throws Throwable {
                    return serializer.serialize(value);
                }
            });
        } catch (QiConversionException except) {
            throw except;
        } catch (Throwable t) {
            throw new QiConversionException(t);
        }
    }

    /**
     * @return True if {@code object} is an instance of {@link Optional} and
     *         {@code targetType} is a specialization of {@link Optional}.
     */
    @Override
    public boolean canDeserialize(Object object, Type targetType) {
        if (!(object instanceof Optional)) return false;

        if (!(targetType instanceof ParameterizedType)) return false;
        ParameterizedType parameterizedType = (ParameterizedType) targetType;
        return Optional.class == parameterizedType.getRawType();
    }

    /**
     * Creates an object of optional type {@code targetType} adapted from an
     * optional object {@code object}. The logic is:
     * <ul>
     * <li>If the optional has a value, recursively adapts the
     * value by calling {@link QiSerializer#deserialize(Object, Type)} on it
     * with the optional value type as the target type.</li>
     * <li>Otherwise, just returns an empty generic {@link Optional}.</li>
     * </ul>
     *
     * @throws QiConversionException
     *         <ul>
     *         <li>If {@code !canDeserialize(object)}, then the cause of the
     *         exception is a {@link IllegalArgumentException}.</li>
     *         <li>If the serialization of the value throws an exception.</li>
     *         </ul>
     * @see #canDeserialize(Object, Type)
     * @see Optional
     * @see Optional#map(Function)
     */
    @Override
    public Optional<?> deserialize(final QiSerializer serializer, Object object,
                                   final Type targetType)
            throws QiConversionException {

        try {
            if (!canDeserialize(object, targetType)) {
                throw new IllegalArgumentException(
                        "The object cannot be deserialized into the target " +
                                "type by this.");
            }

            ParameterizedType parameterizedTargetType =
                    (ParameterizedType) targetType;
            final Type targetValueType =
                    parameterizedTargetType.getActualTypeArguments()[0];

            Optional<?> opt = (Optional<?>) object;
            return opt.map(new Function<Object, Object>() {
                @Override
                public Object execute(Object value) throws Throwable {
                    return serializer.deserialize(value, targetValueType);
                }
            });
        } catch (QiConversionException except) {
            throw except;
        } catch (Throwable t) {
            throw new QiConversionException(t);
        }
    }
}
