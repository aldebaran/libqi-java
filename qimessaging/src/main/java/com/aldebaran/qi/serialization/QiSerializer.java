package com.aldebaran.qi.serialization;

import com.aldebaran.qi.QiConversionException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that provides methods to serialize and deserialize custom objects to and from supported
 * types.
 * <p>
 * By default, there are {@link ArrayConverter}, {@link ListConverter}, {@link MapConverter} and
 * {@link StructConverter}.
 */
public class QiSerializer {
    /**
     * Converter interface which defines the characteristics of a converter.
     * <p>
     * Implement this interface if a custom converter is needed.
     */
    public interface Converter {
        boolean canSerialize(Object object);

        Object serialize(QiSerializer serializer, Object object) throws QiConversionException;

        boolean canDeserialize(Object object, Type targetType);

        Object deserialize(QiSerializer serializer, Object object, Type targetType) throws QiConversionException;
    }

    private static final QiSerializer DEFAULT_INSTANCE = new QiSerializer();

    public static QiSerializer getDefault() {
        return DEFAULT_INSTANCE;
    }

    private List<Converter> converters;

    public QiSerializer(List<Converter> converters) {
        this.converters = converters;
    }

    public QiSerializer() {
        this(createDefaultConverters());
    }

    public void addConverter(Converter converter) {
        converters.add(converter);
    }

    public List<Converter> getConverters() {
        return converters;
    }

    public Object deserialize(Object object, Type targetType) throws QiConversionException {
        if (object == null)
            return null;

        for (Converter converter : converters)
            if (converter.canDeserialize(object, targetType))
                return converter.deserialize(this, object, targetType);

        // do not convert
        return object;
    }

    public Object[] deserialize(Object[] sources, Type[] sourceTypes) throws QiConversionException {
        if (sources.length != sourceTypes.length)
            throw new IllegalArgumentException(
                    "Sources and sourceTypes length don't match (" + sources.length + " != " + sourceTypes.length + ")");
        Object[] converted = new Object[sources.length];
        for (int i = 0; i < sources.length; ++i)
            converted[i] = deserialize(sources[i], sourceTypes[i]);
        return converted;
    }

    public Object serialize(Object object) throws QiConversionException {
        if (object == null)
            return null;

        for (Converter converter : converters)
            if (converter.canSerialize(object))
                return converter.serialize(this, object);

        // do not convert
        return object;
    }

    public static List<Converter> createDefaultConverters() {
        List<Converter> result = new ArrayList<Converter>();
        result.add(new ListConverter());
        result.add(new ArrayConverter());
        result.add(new MapConverter());
        result.add(new StructConverter());
        return result;
    }
}
