package com.aldebaran.qi.serialization;

import com.aldebaran.qi.QiConversionException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Converter for Map serialization.
 */
public class MapConverter implements QiSerializer.Converter {

    @Override
    public boolean canSerialize(Object object) {
        return object instanceof Map;
    }

    @Override
    public Map<?, ?> serialize(QiSerializer serializer, Object object) throws QiConversionException {
        Map<?, ?> map = (Map<?, ?>) object;
        Map<Object, Object> convertedMap = new HashMap<Object, Object>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object convertedKey = serializer.serialize(entry.getKey());
            Object convertedValue = serializer.serialize(entry.getValue());
            convertedMap.put(convertedKey, convertedValue);
        }
        return convertedMap;
    }

    @Override
    public boolean canDeserialize(Object object, Type targetType) {
        if (!(targetType instanceof ParameterizedType))
            return false;

        ParameterizedType parameterizedType = (ParameterizedType) targetType;
        return Map.class == parameterizedType.getRawType();
    }

    @Override
    public Map<?, ?> deserialize(QiSerializer serializer, Object object, Type targetType) throws QiConversionException {
        if (!(object instanceof Map))
            throw new QiConversionException("Cannot convert instance of " + object.getClass() + " to " + targetType);
        Map<?, ?> map = (Map<?, ?>) object;
        ParameterizedType parameterizedType = (ParameterizedType) targetType;
        Type[] types = parameterizedType.getActualTypeArguments();
        Type keyType = types[0];
        Type valueType = types[1];
        Map<Object, Object> convertedMap = new HashMap<Object, Object>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object convertedKey = serializer.deserialize(entry.getKey(), keyType);
            Object convertedValue = serializer.deserialize(entry.getValue(), valueType);
            convertedMap.put(convertedKey, convertedValue);
        }
        return convertedMap;
    }
}
