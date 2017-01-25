package com.aldebaran.qi.serialization;

import com.aldebaran.qi.QiConversionException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Converter for List serialization.
 */
public class ListConverter implements QiSerializer.Converter {

    @Override
    public boolean canSerialize(Object object) {
        return object instanceof List;
    }

    @Override
    public List<?> serialize(QiSerializer serializer, Object object) throws QiConversionException {
        List<?> list = (List<?>) object;
        List<Object> convertedList = new ArrayList<Object>();
        for (Object item : list) {
            Object convertedItem = serializer.serialize(item);
            convertedList.add(convertedItem);
        }
        return convertedList;
    }

    @Override
    public boolean canDeserialize(Object object, Type targetType) {
        if (!(targetType instanceof ParameterizedType))
            return false;

        ParameterizedType parameterizedType = (ParameterizedType) targetType;
        return List.class == parameterizedType.getRawType();
    }

    @Override
    public List<?> deserialize(QiSerializer serializer, Object object, Type targetType) throws QiConversionException {
        if (!(object instanceof List))
            throw new QiConversionException("Cannot convert instance of " + object.getClass() + " to " + targetType);
        List<?> list = (List<?>) object;
        ParameterizedType parameterizedType = (ParameterizedType) targetType;
        Type itemType = parameterizedType.getActualTypeArguments()[0];
        List<Object> convertedList = new ArrayList<Object>();
        for (Object item : list) {
            Object convertedItem = serializer.deserialize(item, itemType);
            convertedList.add(convertedItem);
        }
        return convertedList;
    }
}
