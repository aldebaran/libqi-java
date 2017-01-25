package com.aldebaran.qi.serialization;

import com.aldebaran.qi.QiConversionException;
import com.aldebaran.qi.QiField;
import com.aldebaran.qi.QiStruct;
import com.aldebaran.qi.Tuple;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

/**
 * Converter for Struct and Tuple serialization.
 */
public class StructConverter implements QiSerializer.Converter {

    @Override
    public boolean canSerialize(Object object) {
        return isQiStruct(object.getClass());
    }

    @Override
    public Tuple serialize(QiSerializer serializer, Object object) throws QiConversionException {
        Class<?> cls = object.getClass();
        try {
            Field[] fields = cls.getDeclaredFields();
            int valuesLength = computeMaxTupleIndex(fields) + 1;
            Object[] values = new Object[valuesLength];
            for (Field field : fields) {
                int tupleIndex = getTupleIndex(field);
                if (tupleIndex < 0)
                    continue;
                field.setAccessible(true);
                Object value = field.get(object);
                Object convertedValue = serializer.serialize(value);
                values[tupleIndex] = convertedValue;
            }
            return Tuple.of(values);
        } catch (IllegalAccessException e) {
            throw new QiConversionException(e);
        }
    }

    @Override
    public boolean canDeserialize(Object object, Type targetType) {
        if (!(targetType instanceof Class))
            return false;

        Class<?> cls = (Class<?>) targetType;
        return isQiStruct(cls);
    }

    @Override
    public Object deserialize(QiSerializer serializer, Object object, Type targetType) throws QiConversionException {
        if (!(object instanceof Tuple))
            throw new QiConversionException("Cannot convert instance of " + object.getClass() + " to " + targetType);
        Tuple tuple = (Tuple) object;
        Class<?> targetClass = (Class<?>) targetType;
        try {
            Constructor<?> constructor = targetClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object struct = constructor.newInstance();
            Field[] fields = targetClass.getDeclaredFields();
            for (Field field : fields) {
                int tupleIndex = getTupleIndex(field);
                if (tupleIndex < 0 || tupleIndex >= tuple.size())
                    continue;
                Type fieldType = field.getGenericType();
                Object value = tuple.get(tupleIndex);
                Object convertedValue = serializer.deserialize(value, fieldType);
                field.setAccessible(true);
                field.set(struct, convertedValue);
            }
            return struct;
        } catch (InstantiationException e) {
            throw new QiConversionException(e);
        } catch (IllegalAccessException e) {
            throw new QiConversionException(e);
        } catch (InvocationTargetException e) {
            throw new QiConversionException(e);
        } catch (NoSuchMethodException e) {
            throw new QiConversionException(e);
        }
    }

    private static boolean isQiStruct(Class<?> cls) {
        return cls.getAnnotation(QiStruct.class) != null;
    }

    private static int getTupleIndex(Field field) {
        QiField qiField = field.getAnnotation(QiField.class);
        if (qiField == null)
            return -1;
        return qiField.value();
    }

    private static int computeMaxTupleIndex(Field[] fields) {
        int max = -1;
        for (Field field : fields)
            max = Math.max(max, getTupleIndex(field));
        return max;
    }
}
