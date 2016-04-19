package com.aldebaran.qi;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provide methods to automatically convert (recursively) {@link Tuple}s to
 * {@link QiStruct}s.
 */
public class StructConverter
{
  private StructConverter()
  {
    // not instantiable
  }

  public static Object[] tuplesToStructs(Object[] sources, Type[] sourceTypes) throws QiConversionException
  {
    if (sources.length != sourceTypes.length)
      throw new IllegalArgumentException(
          "Sources and sourceTypes length don't match (" + sources.length + " != " + sourceTypes.length + ")");
    Object[] converted = new Object[sources.length];
    for (int i = 0; i < sources.length; ++i)
      converted[i] = tuplesToStructs(sources[i], sourceTypes[i]);
    return converted;
  }

  public static Object tuplesToStructs(Object source, Type targetType) throws QiConversionException
  {
    if (source == null)
      return null;

    if (targetType instanceof Class)
    {
      Class<?> cls = (Class<?>) targetType;
      if (isQiStruct(cls))
        return tupleToStruct((Tuple) source, cls);
    } else if (targetType instanceof ParameterizedType)
    {
      ParameterizedType parameterizedType = (ParameterizedType) targetType;
      Class<?> rawType = (Class<?>) parameterizedType.getRawType();
      if (List.class == rawType)
      {
        List<?> list = (List<?>) source;
        Type itemType = parameterizedType.getActualTypeArguments()[0];
        return tuplesToStructsInList(list, itemType);
      }
      if (Map.class == rawType)
      {
        Map<?, ?> map = (Map<?, ?>) source;
        Type[] types = parameterizedType.getActualTypeArguments();
        Type keyType = types[0];
        Type valueType = types[1];
        return tuplesToStructsInMap(map, keyType, valueType);
      }
    }

    // do not convert
    return source;
  }

  public static <T> T tupleToStruct(Tuple tuple, Class<T> target) throws QiConversionException
  {
    try
    {
      T struct = target.newInstance();
      Field[] fields = target.getDeclaredFields();
      int commonSize = Math.min(fields.length, tuple.size());
      for (int i = 0; i < commonSize; ++i)
      {
        Field field = fields[i];
        Type fieldType = field.getGenericType();
        Object value = tuple.get(i);
        Object convertedValue = tuplesToStructs(value, fieldType);
        field.setAccessible(true);
        field.set(struct, convertedValue);
      }
      return struct;
    } catch (InstantiationException e)
    {
      throw new QiConversionException(e);
    } catch (IllegalAccessException e)
    {
      throw new QiConversionException(e);
    }
  }

  public static List<?> tuplesToStructsInList(List<?> list, Type itemType) throws QiConversionException
  {
    List<Object> convertedList = new ArrayList<Object>();
    for (Object item : list)
    {
      Object convertedItem = tuplesToStructs(item, itemType);
      convertedList.add(convertedItem);
    }
    return convertedList;
  }

  public static Map<?, ?> tuplesToStructsInMap(Map<?, ?> map, Type keyType, Type valueType) throws QiConversionException
  {
    Map<Object, Object> convertedMap = new HashMap<Object, Object>();
    for (Map.Entry<?, ?> entry : map.entrySet())
    {
      Object convertedKey = tuplesToStructs(entry.getKey(), keyType);
      Object convertedValue = tuplesToStructs(entry.getValue(), valueType);
      convertedMap.put(convertedKey, convertedValue);
    }
    return convertedMap;
  }

  private static boolean isQiStruct(Class<?> cls)
  {
    return cls.getAnnotation(QiStruct.class) != null;
  }
}
