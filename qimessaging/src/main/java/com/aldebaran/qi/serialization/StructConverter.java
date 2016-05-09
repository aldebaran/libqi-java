package com.aldebaran.qi.serialization;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.aldebaran.qi.QiConversionException;
import com.aldebaran.qi.QiStruct;
import com.aldebaran.qi.Tuple;

public class StructConverter implements QiSerializer.Converter
{

  @Override
  public boolean canSerialize(Object object)
  {
    return isQiStruct(object.getClass());
  }

  @Override
  public Tuple serialize(QiSerializer serializer, Object object) throws QiConversionException
  {
    Class<?> cls = object.getClass();
    try
    {
      Field[] fields = cls.getDeclaredFields();
      List<Object> values = new ArrayList<Object>();
      for (Field field : fields)
      {
        if (isTransient(field))
          continue;
        field.setAccessible(true);
        Object value = field.get(object);
        Object convertedValue = serializer.serialize(value);
        values.add(convertedValue);
      }
      return Tuple.of(values.toArray());
    } catch (IllegalAccessException e)
    {
      throw new QiConversionException(e);
    }
  }

  @Override
  public boolean canDeserialize(Object object, Type targetType)
  {
    if (!(targetType instanceof Class))
      return false;

    Class<?> cls = (Class<?>) targetType;
    return isQiStruct(cls);
  }

  @Override
  public Object deserialize(QiSerializer serializer, Object object, Type targetType) throws QiConversionException
  {
    Tuple tuple = (Tuple) object;
    Class<?> targetClass = (Class<?>) targetType;
    try
    {
      Constructor<?> constructor = targetClass.getDeclaredConstructor();
      constructor.setAccessible(true);
      Object struct = constructor.newInstance();
      Field[] fields = targetClass.getDeclaredFields();
      int tupleIndex = 0;
      for (Field field : fields)
      {
        if (tupleIndex >= tuple.size())
          break;
        if (isTransient(field))
          continue;
        Type fieldType = field.getGenericType();
        Object value = tuple.get(tupleIndex++);
        Object convertedValue = serializer.deserialize(value, fieldType);
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
    } catch (InvocationTargetException e)
    {
      throw new QiConversionException(e);
    } catch (NoSuchMethodException e)
    {
      throw new QiConversionException(e);
    }
  }

  private static boolean isQiStruct(Class<?> cls)
  {
    return cls.getAnnotation(QiStruct.class) != null;
  }

  private static boolean isTransient(Field field)
  {
    return (field.getModifiers() & Modifier.TRANSIENT) != 0;
  }
}
