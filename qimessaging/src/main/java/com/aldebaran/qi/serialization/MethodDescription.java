package com.aldebaran.qi.serialization;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Describe a method.<br>
 * It contains the method name, return type and parameters type.
 */
public class MethodDescription {
    /**
     * Couple of next read index and read class.
     */
    static class ClassIndex {
        /**
         * Next index to read.
         */
        final int index;
        /**
         * Read class
         */
        final Class<?> claz;

        /**
         * Create the couple.
         *
         * @param index Next index to read.
         * @param claz  Read class.
         */
        ClassIndex(final int index, final Class<?> claz) {
            this.index = index;
            this.claz = claz;
        }

        /**
         * String representation for debug purpose.
         *
         * @return String representation.
         */
        @Override
        public String toString() {
            return this.index + " :" + this.claz.getName();
        }
    }

    /**
     * "Distance" between primitive type and its Object representation
     */
    private static final int DISTANCE_PRIMITIVE_OBJECT = 1;
    /**
     * "Distance" between numbers: need expand or truncate the value to do the
     * conversion
     */
    private static final int DISTANCE_NUMBERS = 1000;

    /**
     * Read the next class described by characters array at given offset.<br>
     * It returns a couple of read class and next index to read the rest of the
     * characters array.
     *
     * @param offset     Offset where start to read.
     * @param characters Contains a JNI signature.
     * @return Couple of read class and next index to read.
     * @throws IllegalArgumentException If characters array not a valid JNI signature.
     */
    static ClassIndex nextClassIndex(int offset, final char[] characters) {
        Class<?> claz;

        switch (characters[offset]) {
            case 'V':
                claz = void.class;
                break;
            case 'Z':
                claz = boolean.class;
                break;
            case 'B':
                claz = byte.class;
                break;
            case 'C':
                claz = char.class;
                break;
            case 'S':
                claz = short.class;
                break;
            case 'I':
                claz = int.class;
                break;
            case 'J':
                claz = long.class;
                break;
            case 'F':
                claz = float.class;
                break;
            case 'D':
                claz = double.class;
                break;
            case 'L': {
                final int length = characters.length;
                offset++;
                final int start = offset;

                while (offset < length && characters[offset] != ';') {
                    offset++;
                }

                if (offset >= length) {
                    throw new IllegalArgumentException(new String(characters) + " not valid JNI signature");
                }

                String string = new String(characters, start, offset - start);
                string = string.replace('/', '.');

                try {
                    claz = Class.forName(string);
                } catch (final ClassNotFoundException e) {
                    e.printStackTrace();
                    throw new IllegalArgumentException(new String(characters) + " not valid JNI signature", e);
                }
            }
            break;
            case '[': {
                int count = 0;

                while (characters[offset] == '[') {
                    offset++;
                    count++;
                }

                final int[] sizes = new int[count];

                for (int i = 0; i < count; i++) {
                    sizes[i] = 1;
                }

                final ClassIndex classIndex = MethodDescription.nextClassIndex(offset, characters);
                final Object array = Array.newInstance(classIndex.claz, sizes);
                claz = array.getClass();
                offset = classIndex.index - 1;
            }
            break;
            default:
                throw new IllegalArgumentException(new String(characters) + " not valid JNI signature");
        }

        return new ClassIndex(offset + 1, claz);
    }

    /**
     * Compute method description with method name and JNI signature.
     *
     * @param methodName   Method name.
     * @param signatureJNI JNI signature.
     * @return Method description created.
     * @throws IllegalArgumentException If signatureJNI not a valid JNI signature.
     */
    public static MethodDescription fromJNI(final String methodName, final String signatureJNI) {
        final char[] characters = signatureJNI.toCharArray();

        if (characters[0] != '(') {
            throw new IllegalArgumentException(signatureJNI + " not valid JNI signature");
        }

        final List<Class<?>> parametersType = new ArrayList<Class<?>>();
        Class<?> returnType = null;
        final int length = characters.length;
        boolean isParameter = true;
        int offset = 1;
        ClassIndex classIndex;

        while (offset < length) {
            switch (characters[offset]) {
                case ')':
                    isParameter = false;
                    offset++;
                    break;
                default:
                    classIndex = MethodDescription.nextClassIndex(offset, characters);
                    offset = classIndex.index;

                    if (isParameter) {
                        parametersType.add(classIndex.claz);
                    } else if (returnType == null) {
                        returnType = classIndex.claz;
                    } else {
                        throw new IllegalArgumentException(signatureJNI + " not valid JNI signature");
                    }
            }
        }

        if (returnType == null) {
            throw new IllegalArgumentException(signatureJNI + " not valid JNI signature");
        }

        return new MethodDescription(methodName, returnType, parametersType.toArray(new Class[parametersType.size()]));
    }

    /**
     * Method name.
     */
    private final String methodName;
    /**
     * Method return type.
     */
    private final Class<?> returnType;
    /**
     * Method parameters types.
     */
    private final Class<?>[] parametersType;

    /**
     * Create method description.
     *
     * @param methodName     Method name.
     * @param returnType     Method return type.
     * @param parametersType Method parameters types.
     */
    public MethodDescription(final String methodName, final Class<?> returnType, final Class<?>[] parametersType) {
        this.methodName = methodName;
        this.returnType = returnType;
        this.parametersType = parametersType;
    }

    /**
     * Method name.
     *
     * @return Method name.
     */
    public String getMethodName() {
        return this.methodName;
    }

    /**
     * Method return type.
     *
     * @return Method return type.
     */
    public Class<?> getReturnType() {
        return this.returnType;
    }

    /**
     * Method parameters types.
     *
     * @return Method parameters types.
     */
    public Class<?>[] getParametersType() {
        return this.parametersType;
    }

    /**
     * Compute the "distance" between the given method with this description.<br>
     * If "distance" is 0, it means the given method is exactly the method
     * description.<br>
     * If "distance" is {@link Integer#MAX_VALUE}, if means the given method is
     * completely different and incompatible.<br>
     * For other values of "distance" it means the given method is compatible to
     * method description. More the value is near 0, more the compatibility is
     * easy.
     *
     * @param method Method to measure the "distance" with.
     * @return The computed "distance".
     */
    public int distance(final Method method) {
        if (!this.methodName.equals(method.getName())) {
            return Integer.MAX_VALUE;
        }

        final Class<?>[] parameters = method.getParameterTypes();
        final int length = parameters.length;

        if (length != this.parametersType.length) {
            return Integer.MAX_VALUE;
        }

        int distance = MethodDescription.distance(this.returnType, method.getReturnType());

        for (int index = 0; index < length; index++) {
            distance = MethodDescription.addLimited(distance,
                    MethodDescription.distance(this.parametersType[index], parameters[index]));
        }

        return distance;
    }

    /**
     * Add two positive (or zero) integers.<br>
     * The addition result guarantees to be limited to {@link Integer#MAX_VALUE}
     * .<br>
     * It avoids the overflow issue.
     *
     * @param integer1 First integer.
     * @param integer2 Second integer.
     * @return The addition.
     */
    private static int addLimited(final int integer1, final int integer2) {
        assert integer1 >= 0 && integer2 >= 0;

        if (integer1 >= Integer.MAX_VALUE - integer2) {
            return Integer.MAX_VALUE;
        }

        return integer1 + integer2;
    }

    /**
     * Compute the "distance" between two class.<br>
     * If "distance" is 0, it means the classes are the same class.<br>
     * If "distance" is {@link Integer#MAX_VALUE}, if means the classes are
     * completely different and incompatible.<br>
     * For other values of "distance" it means the given the classes are
     * compatible. More the value is near 0, more the compatibility is easy.
     *
     * @param class1 First class.
     * @param class2 Second class.
     * @return Computed "distance".
     */
    private static int distance(final Class<?> class1, final Class<?> class2) {
        if (class1.equals(class2)) {
            return 0;
        }

        if (SignatureUtilities.isVoid(class1) && SignatureUtilities.isVoid(class2)) {
            return MethodDescription.DISTANCE_PRIMITIVE_OBJECT;
        }

        if (SignatureUtilities.isBoolean(class1) && SignatureUtilities.isBoolean(class2)) {
            return MethodDescription.DISTANCE_PRIMITIVE_OBJECT;
        }

        if (SignatureUtilities.isCharacter(class1) && SignatureUtilities.isCharacter(class2)) {
            return MethodDescription.DISTANCE_PRIMITIVE_OBJECT;
        }

        if (SignatureUtilities.isByte(class1) && SignatureUtilities.isByte(class2)) {
            return MethodDescription.DISTANCE_PRIMITIVE_OBJECT;
        }

        if (SignatureUtilities.isShort(class1) && SignatureUtilities.isShort(class2)) {
            return MethodDescription.DISTANCE_PRIMITIVE_OBJECT;
        }

        if (SignatureUtilities.isInteger(class1) && SignatureUtilities.isInteger(class2)) {
            return MethodDescription.DISTANCE_PRIMITIVE_OBJECT;
        }

        if (SignatureUtilities.isLong(class1) && SignatureUtilities.isLong(class2)) {
            return MethodDescription.DISTANCE_PRIMITIVE_OBJECT;
        }

        if (SignatureUtilities.isFloat(class1) && SignatureUtilities.isFloat(class2)) {
            return MethodDescription.DISTANCE_PRIMITIVE_OBJECT;
        }

        if (SignatureUtilities.isDouble(class1) && SignatureUtilities.isDouble(class2)) {
            return MethodDescription.DISTANCE_PRIMITIVE_OBJECT;
        }

        if (SignatureUtilities.isNumber(class1) && SignatureUtilities.isNumber(class2)) {
            return MethodDescription.DISTANCE_NUMBERS;
        }

        return Integer.MAX_VALUE;
    }

    /**
     * String description for debug purpose
     *
     * @return String description for debug purpose
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.methodName);
        stringBuilder.append('(');

        if (this.parametersType != null) {
            boolean first = true;

            for (Class<?> clazz : this.parametersType) {
                if (!first) {
                    stringBuilder.append(", ");
                }

                first = false;
                stringBuilder.append(clazz.getName());
            }
        }

        stringBuilder.append("):");

        if (SignatureUtilities.isVoid(this.returnType)) {
            stringBuilder.append("void");
        }
        else {
            stringBuilder.append(this.returnType.getName());
        }

        return stringBuilder.toString();
    }
}
