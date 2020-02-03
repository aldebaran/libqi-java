package com.aldebaran.qi.serialization;

import com.aldebaran.qi.QiConversionException;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;

public class ByteBufferConverter implements QiSerializer.Converter {
    @Override
    public boolean canSerialize(Object object) {
        return object instanceof ByteBuffer;
    }

    // Precondition: canSerialize(object)
    @Override
    public Object serialize(QiSerializer serializer, Object object) throws QiConversionException {
        return object;
    }

    @Override
    public boolean canDeserialize(Object object, Type targetType) {
        return object instanceof byte[] && ByteBuffer.class.isAssignableFrom((Class<?>) targetType);
    }

    // Precondition: canDeserialize(object, targetType)
    @Override
    public Object deserialize(QiSerializer serializer, Object object, Type targetType) throws QiConversionException {
        return ByteBuffer.wrap((byte[]) object);
    }
}
