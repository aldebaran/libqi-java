package com.aldebaran.qi;

import com.aldebaran.qi.serialization.QiSerializer;
import com.aldebaran.qi.serialization.StructConverter;

import java.util.Arrays;

/**
 * Class that represents a list of values of different fixed types.
 */
public class Tuple {
    private Object[] values;

    /**
     * Create a tuple from its values.
     * <p>
     * Expose it as a static method to avoid conflicts with {@link #Tuple(int)
     * Tuple(size)}.
     *
     * @param values the values
     * @return the tuple
     */
    public static Tuple of(Object... values) {
        return new Tuple(values);
    }

    public static Tuple fromStruct(Object struct) throws QiConversionException {
        return new StructConverter().serialize(QiSerializer.getDefault(), struct);
    }

    // called from native
    private Tuple(Object... values) {
        this.values = values;
    }

    public Tuple(int size) {
        values = new Object[size];
    }

    public int size() {
        return values.length;
    }

    public Object get(int index) {
        return values[index];
    }

    public <T> void set(int index, T value) {
        values[index] = value;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("(");
        boolean first = true;
        for (Object value : values) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append(value);
        }
        builder.append(')');
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(values);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Tuple other = (Tuple) obj;
        if (!Arrays.equals(values, other.values))
            return false;
        return true;
    }
}
