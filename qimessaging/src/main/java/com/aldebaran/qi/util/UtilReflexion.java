package com.aldebaran.qi.util;

public class UtilReflexion {

    public static Object defaultValue(Class clazz) {
        if (boolean.class.equals(clazz) || Boolean.class.equals(clazz)) {
            return false;
        }

        if (char.class.equals(clazz) || Character.class.equals(clazz)) {
            return ' ';
        }

        if (byte.class.equals(clazz) || Byte.class.equals(clazz)) {
            return (byte) 0;
        }

        if (short.class.equals(clazz) || Short.class.equals(clazz)) {
            return (short) 0;
        }

        if (int.class.equals(clazz) || Integer.class.equals(clazz)) {
            return 0;
        }

        if (long.class.equals(clazz) || Long.class.equals(clazz)) {
            return 0L;
        }

        if (float.class.equals(clazz) || Float.class.equals(clazz)) {
            return 0f;
        }

        if (double.class.equals(clazz) || Double.class.equals(clazz)) {
            return 0.0;
        }

        return null;
    }
}
