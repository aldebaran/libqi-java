package com.aldebaran.qi;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Class used to keep and extract a parameter type at runtime.
 *
 * To retrieve the type at runtime, subclass this {@code TypeToken} and call
 * {@link #getType()}:
 *
 * <pre>
 * Type listOfStringsType = new TypeToken<List<String>>() {}.getType();
 * </pre>
 *
 * Inspired from GSON {@code TypeToken}:
 * <https://google.github.io/gson/apidocs/com/google/gson/reflect/TypeToken.html>
 *
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
