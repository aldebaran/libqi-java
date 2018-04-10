package com.aldebaran.qi.serialization;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.aldebaran.qi.AnyObject;
import com.aldebaran.qi.Future;
import com.aldebaran.qi.QiConversionException;
import com.aldebaran.qi.QiField;
import com.aldebaran.qi.QiStruct;
import com.aldebaran.qi.Tuple;

/**
 * Utilities for manipulates signatures
 */
public class SignatureUtilities {
    /**
     * Libqi boolean signature
     */
    public static final String BOOLEAN = "b";
    /**
     * Libqi char signature
     */
    public static final String CHARACTER = "c";
    /**
     * Libqi void signature
     */
    public static final String VOID = "v";
    /**
     * Libqi int signature
     */
    public static final String INTEGER = "i";
    /**
     * Libqi long signature
     */
    public static final String LONG = "l";
    /**
     * Libqi float signature
     */
    public static final String FLOAT = "f";
    /**
     * Libqi double signature
     */
    public static final String DOUBLE = "d";
    /**
     * Libqi String signature
     */
    public static final String STRING = "s";
    /**
     * Libqi Object signature
     */
    public static final String OBJECT = "o";

    /**
     * Compute libqi signature for given method
     *
     * @param method Method to get the signature from
     * @return Libqi signature for given method
     */
    public static String computeSignatureForMethod(final Method method) {
        final StringBuilder stringBuilder = new StringBuilder();

        // Signature header
        stringBuilder.append(method.getName());
        stringBuilder.append("::");

        // Return type signature
        SignatureUtilities.computeSignatureForReturnType(method.getReturnType(), method.getGenericReturnType(), stringBuilder);

        // Parameters signature
        stringBuilder.append("(");
        final Class<?>[] parametersClasses = method.getParameterTypes();
        final Type[] parametersTypes = method.getGenericParameterTypes();

        if (parametersClasses != null) {
            final int size = parametersClasses.length;

            for (int index = 0; index < size; index++) {
                SignatureUtilities.computeSignature(parametersClasses[index], parametersTypes[index], stringBuilder);
            }
        }

        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    /**
     * Compute and append signature for argument of parameterized type
     * (List&lt;String&gt;, Map&lt;Integer, String&gt;, ...)
     *
     * @param index             Index of argument in parameterized type
     * @param parameterizedType Parameterized type to extract the argument
     * @param stringBuilder     String builder where append signature
     */
    private static void computeSignature(final int index, final ParameterizedType parameterizedType,
                                         final StringBuilder stringBuilder) {
        final Type argument = parameterizedType.getActualTypeArguments()[index];

        if (argument instanceof ParameterizedType) {
            SignatureUtilities
                    .computeSignature((Class<?>) ((ParameterizedType) argument).getRawType(), argument, stringBuilder);
        } else if (argument instanceof Class) {
            SignatureUtilities.computeSignature((Class<?>) argument, argument, stringBuilder);
        }
    }

    /**
     * Indicates if given class can be considered as Void or void
     *
     * @param clazz Class to test
     * @return {@code true} if given class can be considered as Void or void
     */
    public static boolean isVoid(Class<?> clazz) {
        return clazz == null || void.class.equals(clazz) || Void.class.equals(clazz);
    }

    /**
     * Indicates if given class is Boolean or boolean
     *
     * @param clazz Class to test
     * @return {@code true} if given class is Boolean or boolean
     */
    public static boolean isBoolean(Class<?> clazz) {
        return boolean.class.equals(clazz) || Boolean.class.equals(clazz);
    }

    /**
     * Indicates if given class is Character or char
     *
     * @param clazz Class to test
     * @return {@code true} if given class is Character or char
     */
    public static boolean isCharacter(Class<?> clazz) {
        return char.class.equals(clazz) || Character.class.equals(clazz);
    }

    /**
     * Indicates if given class is Byte or byte
     *
     * @param clazz Class to test
     * @return {@code true} if given class is Byte or byte
     */
    public static boolean isByte(Class<?> clazz) {
        return byte.class.equals(clazz) || Byte.class.equals(clazz);
    }

    /**
     * Indicates if given class is Short or short
     *
     * @param clazz Class to test
     * @return {@code true} if given class is Short or short
     */
    public static boolean isShort(Class<?> clazz) {
        return short.class.equals(clazz) || Short.class.equals(clazz);
    }

    /**
     * Indicates if given class is Integer or int
     *
     * @param clazz Class to test
     * @return {@code true} if given class is Integer or int
     */
    public static boolean isInteger(Class<?> clazz) {
        return int.class.equals(clazz) || Integer.class.equals(clazz);
    }

    /**
     * Indicates if given class is Long or long
     *
     * @param clazz Class to test
     * @return {@code true} if given class is Long or long
     */
    public static boolean isLong(Class<?> clazz) {
        return long.class.equals(clazz) || Long.class.equals(clazz);
    }

    /**
     * Indicates if given class is Float or float
     *
     * @param clazz Class to test
     * @return {@code true} if given class is Float or float
     */
    public static boolean isFloat(Class<?> clazz) {
        return float.class.equals(clazz) || Float.class.equals(clazz);
    }

    /**
     * Indicates if given class is Double or double
     *
     * @param clazz Class to test
     * @return {@code true} if given class is Double or double
     */
    public static boolean isDouble(Class<?> clazz) {
        return double.class.equals(clazz) || Double.class.equals(clazz);
    }

    /**
     * Indicates if given class is a Number
     *
     * @param clazz Class to test
     * @return {@code true} if given class is a Number
     */
    public static boolean isNumber(Class<?> clazz) {
        return SignatureUtilities.isByte(clazz) || SignatureUtilities.isShort(clazz) || SignatureUtilities.isInteger(clazz)
                || SignatureUtilities.isLong(clazz) || SignatureUtilities.isFloat(clazz) || SignatureUtilities.isDouble(clazz);
    }

    /**
     * Compute and append signature for given class and corresponding type specialized on return type
     *
     * @param clazz         Class to get signature
     * @param type          Type corresponds to given class
     * @param stringBuilder String builder where append signature
     */
    private static void computeSignatureForReturnType(Class<?> clazz, Type type, final StringBuilder stringBuilder) {
        if (Future.class.isAssignableFrom(clazz)) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            type = parameterizedType.getActualTypeArguments()[0];

            if (type instanceof ParameterizedType) {
                clazz = (Class) ((ParameterizedType) type).getRawType();
            }
            else if (type instanceof Class) {
                clazz = (Class) type;
            }
            else {
                //Just a warning for a not managed type.
                //Print in error stream to attract attention
                System.err.println("Warning! While computing signature for return type " + type + ":" + type.getClass().getName() + " TO Class");
            }
        }

        SignatureUtilities.computeSignature(clazz, type, stringBuilder);
    }

    /**
     * Compute and append signature for given class and corresponding type
     *
     * @param clazz         Class to get signature
     * @param type          Type corresponds to given class
     * @param stringBuilder String builder where append signature
     */
    private static void computeSignature(final Class<?> clazz, final Type type, final StringBuilder stringBuilder) {
        if (SignatureUtilities.isVoid(clazz)) {
            stringBuilder.append(SignatureUtilities.VOID);
        } else if (SignatureUtilities.isBoolean(clazz)) {
            stringBuilder.append(SignatureUtilities.BOOLEAN);
        } else if (SignatureUtilities.isCharacter(clazz)) {
            stringBuilder.append(SignatureUtilities.CHARACTER);
        } else if (SignatureUtilities.isInteger(clazz) || clazz.isEnum()) {
            stringBuilder.append(SignatureUtilities.INTEGER);
        } else if (SignatureUtilities.isLong(clazz)) {
            stringBuilder.append(SignatureUtilities.LONG);
        } else if (SignatureUtilities.isFloat(clazz)) {
            stringBuilder.append(SignatureUtilities.FLOAT);
        } else if (SignatureUtilities.isDouble(clazz)) {
            stringBuilder.append(SignatureUtilities.DOUBLE);
        } else if (String.class.equals(clazz)) {
            stringBuilder.append(SignatureUtilities.STRING);
        } else if (List.class.isAssignableFrom(clazz)) {
            stringBuilder.append("[");
            SignatureUtilities.computeSignature(0, (ParameterizedType) type, stringBuilder);
            stringBuilder.append("]");
        } else if (Map.class.isAssignableFrom(clazz)) {
            stringBuilder.append("{");
            SignatureUtilities.computeSignature(0, (ParameterizedType) type, stringBuilder);
            SignatureUtilities.computeSignature(1, (ParameterizedType) type, stringBuilder);
            stringBuilder.append("}");
        } else {
            final QiStruct struct = clazz.getAnnotation(QiStruct.class);
            if (struct != null) {
                final List<QiFieldInformation> qiFieldInformations = SignatureUtilities.collectSortedQiFieldInformation(clazz);
                stringBuilder.append("(");

                for (final QiFieldInformation information : qiFieldInformations) {
                    SignatureUtilities.computeSignature(information.clazz, information.type, stringBuilder);
                }

                stringBuilder.append(")");
            }
            else if (Tuple.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException(clazz.getName() + " not annotated as " + QiStruct.class.getName());
            }
            else {
                stringBuilder.append(SignatureUtilities.OBJECT);
            }
        }
    }

    /**
     * Collect all QiFieldInformation from {@link QiField QiField(s)} in a
     * {@link QiStruct}.<br>
     * The list is sorted by {@link QiField} order. <br>
     * The returned list is empty if given class is not a {@link QiStruct}
     *
     * @param clazz
     *            {@link QiStruct} to collect its {@link QiField QiField(s)}
     * @return Collected {@link QiField QiField(s)} information
     */
    public static List<QiFieldInformation> collectSortedQiFieldInformation(Class<?> clazz) {
        final List<QiFieldInformation> qiFieldInformations = new ArrayList<QiFieldInformation>();
        final QiStruct struct = clazz.getAnnotation(QiStruct.class);

        if (struct == null) {
            return qiFieldInformations;
        }

        QiFieldInformation qiFieldInformation;

        for (final Field field : clazz.getDeclaredFields()) {
            qiFieldInformation = QiFieldInformation.createInformation(field);

            if (qiFieldInformation != null) {
                qiFieldInformations.add(qiFieldInformation);
            }
        }

        Collections.sort(qiFieldInformations);
        return qiFieldInformations;
    }

    /**
     * Convert a primitive type to its Object type.
     *
     * @param type Native type.
     * @return Object type.
     */
    public static Type convertNativeTypeToObjectType(final Type type) {
        if (type == null || void.class.equals(type)) {
            return Void.class;
        }

        if (boolean.class.equals(type)) {
            return Boolean.class;
        }

        if (char.class.equals(type)) {
            return Character.class;
        }

        if (int.class.equals(type)) {
            return Integer.class;
        }

        if (long.class.equals(type)) {
            return Long.class;
        }

        if (float.class.equals(type)) {
            return Float.class;
        }

        if (double.class.equals(type)) {
            return Double.class;
        }

        return type;
    }

    /**
     * Convert an array of types, if the array contains primitive types they are
     * convert to their corresponding Object type.
     *
     * @param types Array to convert
     * @return Converted array
     */
    public static Type[] convertNativeTypeToObjectType(final Type... types) {
        if (types == null) {
            return new Type[0];
        }

        final int length = types.length;
        final Type[] results = new Type[length];

        for (int i = 0; i < length; i++) {
            results[i] = SignatureUtilities.convertNativeTypeToObjectType(types[i]);
        }

        return results;
    }

    /**
     * Try to convert a value to a desired type
     *
     * @param value Value to convert
     * @param to    Destination type
     * @return Converted value
     */
    public static Object convert(Object value, final Class<?> to) {
        if (value == null) {
            return null;
        }

        Class<?> from = value.getClass();

        if (from.equals(to)) {
            return value;
        }

        if (boolean.class.equals(from) && Boolean.class.equals(to)) {
            return value;
        }

        if (Boolean.class.equals(from) && boolean.class.equals(to)) {
            return ((Boolean) value).booleanValue();
        }

        if (char.class.equals(from) && Character.class.equals(to)) {
            return value;
        }

        if (Character.class.equals(from) && char.class.equals(to)) {
            return ((Character) value).charValue();
        }

        if (byte.class.equals(from)) {
            final Byte b = (Byte) value;
            from = Byte.class;
            value = b;
        }

        if (short.class.equals(from)) {
            final Short b = (Short) value;
            from = Short.class;
            value = b;
        }

        if (int.class.equals(from)) {
            final Integer b = (Integer) value;
            from = Integer.class;
            value = b;
        }

        if (long.class.equals(from)) {
            final Long b = (Long) value;
            from = Long.class;
            value = b;
        }

        if (float.class.equals(from)) {
            final Float b = (Float) value;
            from = Float.class;
            value = b;
        }

        if (double.class.equals(from)) {
            final Double b = (Double) value;
            from = Double.class;
            value = b;
        }

        if (Number.class.isAssignableFrom(from)) {
            final Number number = (Number) value;

            if (byte.class.equals(to)) {
                return number.byteValue();
            }

            if (short.class.equals(to)) {
                return number.shortValue();
            }

            if (int.class.equals(to)) {
                return number.intValue();
            }

            if (long.class.equals(to)) {
                return number.longValue();
            }

            if (float.class.equals(to)) {
                return number.floatValue();
            }

            if (double.class.equals(to)) {
                return number.doubleValue();
            }
        }

        if (Number.class.isAssignableFrom(from) && Number.class.isAssignableFrom(to)) {
            final Number number = (Number) value;

            if (Byte.class.equals(to)) {
                return new Byte(number.byteValue());
            }

            if (Short.class.equals(to)) {
                return new Short(number.shortValue());
            }

            if (Integer.class.equals(to)) {
                return new Integer(number.intValue());
            }

            if (Long.class.equals(to)) {
                return new Long(number.longValue());
            }

            if (Float.class.equals(to)) {
                return new Float(number.floatValue());
            }

            if (Double.class.equals(to)) {
                return new Double(number.doubleValue());
            }
        }

        // Convert Tuple to QiStruct
        if ((value instanceof Tuple) && to.isAnnotationPresent(QiStruct.class)) {
            final Tuple tuple = (Tuple) value;
            final int size = tuple.size();
            final List<QiFieldInformation> qiFieldInformations = SignatureUtilities.collectSortedQiFieldInformation(to);

            // Only match for same size
            if (size == qiFieldInformations.size()) {
                QiFieldInformation qiFieldInformation;
                Object valuetoSet;
                Field field;

                try {
                    // Create the QiStruct instance to fill
                    final Constructor constructor = to.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    final Object instance = constructor.newInstance();

                    // Fill each QiStruct field
                    for (int index = 0; index < size; index++) {
                        qiFieldInformation = qiFieldInformations.get(index);
                        valuetoSet = SignatureUtilities.convert(tuple.get(index), qiFieldInformation.clazz);
                        field = to.getDeclaredField(qiFieldInformation.name);
                        field.setAccessible(true);
                        field.set(instance, valuetoSet);
                    }

                    return instance;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return value;
                }
            }
        }

        // Convert integer to enumeration
        if ((int.class.equals(from) || Integer.class.equals(from)) && to.isEnum()) {
            try {
                final Method getQiValue = to.getDeclaredMethod("getQiValue");
                getQiValue.setAccessible(true);
                final Method valuesMethod = to.getDeclaredMethod("values");
                final Object values = valuesMethod.invoke(null);
                final int size = Array.getLength(values);
                Object object;

                for (int index = 0; index < size; index++) {
                    object = Array.get(values, index);

                    if (getQiValue.invoke(object).equals(value)) {
                        return object;
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                return value;
            }
        }

        if (AnyObject.class.equals(from)) {
            try {
                return QiSerializer.getDefault().deserialize(value, to);
            }
            catch (QiConversionException e) {
                System.err.println("Issue while embed any object");
                e.printStackTrace();
            }
        }

        return value;
    }

    /**
     * Convert a value from Java to value that can be sent to libqi.
     *
     * @param object      Object to convert.
     * @param desiredType Destination type.
     * @return Converted value.
     */
    public static Object convertValueJavaToLibQI(Object object, final Type desiredType) {
        if (object == null) {
            return null;
        }

        final Class<?> clazz = object.getClass();

        if (clazz.equals(desiredType)) {
            return object;
        }

        if (desiredType instanceof Class) {
            final Class<?> desiredClass = (Class<?>) desiredType;

            if (Number.class.isAssignableFrom(clazz) && Number.class.isAssignableFrom(desiredClass)) {
                final Number number = (Number) object;

                if (Byte.class.equals(desiredClass)) {
                    return new Byte(number.byteValue());
                }

                if (Short.class.equals(desiredClass)) {
                    return new Short(number.shortValue());
                }

                if (Integer.class.equals(desiredClass)) {
                    return new Integer(number.intValue());
                }

                if (Long.class.equals(desiredClass)) {
                    return new Long(number.longValue());
                }

                if (Float.class.equals(desiredClass)) {
                    return new Float(number.floatValue());
                }

                if (Double.class.equals(desiredClass)) {
                    return new Double(number.doubleValue());
                }
            }
        }

        try {
            object = QiSerializer.getDefault().serialize(object);
        }
        catch (QiConversionException conversionException) {
            System.err.println("Issue whille serialization!");
            conversionException.printStackTrace();
        }

        return object;
    }
}
