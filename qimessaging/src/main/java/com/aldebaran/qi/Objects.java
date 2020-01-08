package com.aldebaran.qi;

import java.util.Arrays;
import java.util.Comparator;


/**
 * For documentation purpose, please refer to
 * https://docs.oracle.com/javase/8/docs/api/java/util/Objects.html
 * TODO : remove when java 1.7
 */
class Objects {
    static boolean isNull(Object obj) {
        return obj == null;
    }

    static boolean nonNull(Object obj) {
        return obj != null;
    }

    static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    static int hashCode(Object obj) {
        return nonNull(obj) ? obj.hashCode() : 0;
    }

    static int hash(Object... values) {
        return Arrays.hashCode(values);
    }

    static String toString(Object obj) {
        return String.valueOf(obj);
    }

    static String toString(Object obj, String nullDefault) {
        return nonNull(obj) ? obj.toString() : nullDefault;
    }

    static <T> int compare(T a, T b, Comparator<? super T> c) {
        return (a == b) ? 0 :  c.compare(a, b);
    }

    static <T> T requireNonNull(T obj) {
        if (isNull(obj))
            throw new NullPointerException();
        return obj;
    }

    static <T> T requireNonNull(T obj, String message) {
        if (isNull(obj))
            throw new NullPointerException(message);
        return obj;
    }

    static <T> T requireNonNullElse(T obj, T defaultObj) {
        return nonNull(obj) ? obj : requireNonNull(defaultObj, "defaultObj");
    }

    static <T> T requireNonNullElseGet(T obj, Supplier<? extends T> supplier) {
        return nonNull(obj) ? obj
                : requireNonNull(requireNonNull(supplier, "supplier").get(), "supplier.get()");
    }

    static <T> T requireNonNull(T obj, Supplier<String> messageSupplier) {
        if (isNull(obj))
            throw new NullPointerException(requireNonNull(messageSupplier, "messageSupplier").get());
        return obj;
    }

    @SuppressWarnings("unchecked")
    static <Out, In> Out uncheckedCast(In input) {
        return (Out)input;
    }

}
