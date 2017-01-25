package com.aldebaran.qi;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Class used to represent a generic type {@code T} and to extract its type
 * information at runtime.
 * <p>
 * To retrieve the type at runtime, create a subclass and call {@link #getType()}:
 * <p>
 * <pre>
 * Type listOfStringsType = new TypeToken<List<String>>() {}.getType();
 * </pre>
 * <p>
 * Inspired by GSON's {@code TypeToken}.
 *
 * @param <T> the type to retrieve at runtime
 * @see <a href="https://google.github.io/gson/apidocs/com/google/gson/reflect/TypeToken.html">
 * GSON's TypeToken</a>
 */
public abstract class TypeToken<T> {
    public Type getType() {
        ParameterizedType supertype = (ParameterizedType) getClass().getGenericSuperclass();
        return supertype.getActualTypeArguments()[0];
    }
}
