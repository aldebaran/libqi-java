package com.aldebaran.qi;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Class used to represent a generic type {@code T} and to extract its type
 * information at runtime.
 * <p>
 * To retrieve the type at runtime, create a subclass and call {@link #getType()}:
 *
 * <pre>
 * Type listOfStringsType = new TypeToken<List<String>>() {}.getType();
 * </pre>
 *
 * Inspired by GSON's {@code TypeToken}.
 * @see <a href="https://google.github.io/gson/apidocs/com/google/gson/reflect/TypeToken.html">
 * GSON's TypeToken</a>
 * @param <T>
 *          the type to retrieve at runtime
 */
public abstract class TypeToken<T>
{
  public Type getType()
  {
    ParameterizedType supertype = (ParameterizedType) getClass().getGenericSuperclass();
    return supertype.getActualTypeArguments()[0];
  }
}
