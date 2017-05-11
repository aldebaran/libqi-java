package com.aldebaran.qi;

import org.junit.Assert;
import org.junit.Test;

import com.aldebaran.qi.serialization.MethodDescription;

public class MethodDescriptionTests {
    @Test
    public void test_fromJNI_void_Parameters_empty() {
        MethodDescription methodDescription = MethodDescription.fromJNI("foo", "()V");
        Assert.assertEquals("foo", methodDescription.getMethodName());
        Assert.assertEquals(void.class, methodDescription.getReturnType());
        Class[] parameters = methodDescription.getParametersType();
        Assert.assertEquals(0, parameters.length);
    }

    @Test
    public void test_fromJNI_boolean_Parameters_StringArray_int_long() {
        MethodDescription methodDescription = MethodDescription.fromJNI("bar", "([[Ljava/lang/String;IJ)Z");
        Assert.assertEquals("bar", methodDescription.getMethodName());
        Assert.assertEquals(boolean.class, methodDescription.getReturnType());
        Class[] parameters = methodDescription.getParametersType();
        Assert.assertEquals(3, parameters.length);
        String[][] array = new String[1][1];
        Assert.assertEquals(array.getClass(), parameters[0]);
        Assert.assertEquals(int.class, parameters[1]);
        Assert.assertEquals(long.class, parameters[2]);
    }

    @Test
    public void test_fromJNI_Object_Parameters_String() {
        MethodDescription methodDescription = MethodDescription.fromJNI("method", "(Ljava/lang/String;)Ljava/lang/Object;");
        Assert.assertEquals("method", methodDescription.getMethodName());
        Assert.assertEquals(Object.class, methodDescription.getReturnType());
        Class[] parameters = methodDescription.getParametersType();
        Assert.assertEquals(1, parameters.length);
        Assert.assertEquals(String.class, parameters[0]);
    }

    @Test
    public void test_fromJNI_ObjectArray_Parameters_String() {
        MethodDescription methodDescription = MethodDescription.fromJNI("method", "(Ljava/lang/String;)[Ljava/lang/Object;");
        Assert.assertEquals("method", methodDescription.getMethodName());
        Object[] objects = new Object[1];
        Assert.assertEquals(objects.getClass(), methodDescription.getReturnType());
        Class[] parameters = methodDescription.getParametersType();
        Assert.assertEquals(1, parameters.length);
        Assert.assertEquals(String.class, parameters[0]);
    }

    @Test
    public void test_fromJNI_longArray_Parameters_intArray() {
        MethodDescription methodDescription = MethodDescription.fromJNI("toLong", "([I)[J");
        Assert.assertEquals("toLong", methodDescription.getMethodName());
        long[] longs = new long[1];
        Assert.assertEquals(longs.getClass(), methodDescription.getReturnType());
        Class[] parameters = methodDescription.getParametersType();
        Assert.assertEquals(1, parameters.length);
        int[] ints = new int[1];
        Assert.assertEquals(ints.getClass(), parameters[0]);
    }
}
