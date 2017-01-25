package com.aldebaran.qi.serialization;

import com.aldebaran.qi.QiConversionException;

import java.lang.reflect.Array;
import java.lang.reflect.Type;

/**
 * Converter for Array serialization.
 */
public class ArrayConverter implements QiSerializer.Converter {

    @Override
    public boolean canSerialize(Object object) {
        return object instanceof Object[];
    }

    @Override
    public Object[] serialize(QiSerializer serializer, Object object) throws QiConversionException {
        Object[] array = (Object[]) object;
        Object[] convertedArray = new Object[array.length];
        for (int i = 0; i < array.length; ++i)
            convertedArray[i] = serializer.serialize(array[i]);
        return convertedArray;
    }

    @Override
    public boolean canDeserialize(Object object, Type targetType) {
        if (!(targetType instanceof Class))
            return false;

        Class<?> cls = (Class<?>) targetType;
        return cls.isArray();
    }

    @Override
    public Object[] deserialize(QiSerializer serializer, Object object, Type targetType) throws QiConversionException {
        if (!(object instanceof Object[]))
            throw new QiConversionException("Cannot convert instance of " + object.getClass() + " to " + targetType);
        Object[] array = (Object[]) object;
        Class<?> cls = (Class<?>) targetType;
        Class<?> componentType = cls.getComponentType();
        Object[] convertedArray = (Object[]) Array.newInstance(componentType, array.length);
        for (int i = 0; i < array.length; ++i)
            convertedArray[i] = serializer.deserialize(array[i], componentType);
        return convertedArray;
    }
}
