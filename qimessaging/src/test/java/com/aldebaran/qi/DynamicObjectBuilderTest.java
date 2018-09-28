/*
 **  Copyright (C) 2018 Softbank Robotics Europe
 **  See COPYING for the license
 */
package com.aldebaran.qi;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DynamicObjectBuilderTest {

    private DynamicObjectBuilder builder;

    @Before
    public void setUp() {
        builder = new DynamicObjectBuilder();
    }

    @After
    public void tearDown() {
        builder = null;
    }

    private void runAdvertisePropertyUsesValueTypeSignature(Property<?> property,
                                                            String signature) {
        final String propertyName = "muffins";
        builder.advertiseProperty(propertyName, property);

        final AnyObject object = builder.object();
        final String objectStr = object.toString();
        Assert.assertTrue(objectStr.contains(String.format("Properties\n  100 %s %s",
                propertyName, signature)));
        Assert.assertTrue(objectStr.contains(String.format("Signals\n  100 %s (%s)", propertyName
                , signature)));
    }


    @Test
    public void advertisePropertyUsesValueTypeSignature_String() {
        runAdvertisePropertyUsesValueTypeSignature(new Property<String>(String.class), "String");
    }

    @Test
    public void advertisePropertyUsesValueTypeSignature_Int32() {
        runAdvertisePropertyUsesValueTypeSignature(new Property<Integer>(Integer.class), "Int32");
    }

    @Test
    public void advertisePropertyUsesValueTypeSignature_Boolean() {
        runAdvertisePropertyUsesValueTypeSignature(new Property<Boolean>(Boolean.class), "Bool");
    }

    @Test
    public void advertisePropertyUsesValueTypeSignature_Int64() {
        runAdvertisePropertyUsesValueTypeSignature(new Property<Long>(Long.class), "Int64");
    }

    @Test
    public void advertisePropertyUsesValueTypeSignature_Float() {
        runAdvertisePropertyUsesValueTypeSignature(new Property<Float>(Float.class), "Float");
    }

    @Test
    public void advertisePropertyUsesValueTypeSignature_Double() {
        runAdvertisePropertyUsesValueTypeSignature(new Property<Double>(Double.class), "Double");
    }
}
