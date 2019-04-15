package com.aldebaran.qi;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class AnyObjectTest {
    public AnyObject factory = null;

    @Before
    public void setUp() {
        DynamicObjectBuilder ob = new DynamicObjectBuilder();

        // Get instance of ReplyService
        QiService reply = new ReplyService();

        // Register method 'createObject'
        ob.advertiseMethod("createObject::o()", reply, "Return a test object");

        factory = ob.object();
    }

    @Test
    public void testHashCodeSameObject() {
        AnyObject anyObject = null;

        try {
            anyObject = factory.<AnyObject>call("createObject").get();
        } catch (Exception e) {
            fail("Call must not fail: " + e.getMessage());
        }

        assertEquals(anyObject.hashCode(), anyObject.hashCode());
        assertEquals(anyObject, anyObject);
    }

    @Test
    public void testHashCodeDifferentObject() {
        AnyObject anyObject = null, anyOtherObject = null;

        try {
            anyObject = factory.<AnyObject>call("createObject").get();
            anyOtherObject = factory.<AnyObject>call("createObject").get();
        } catch (Exception e) {
            fail("Call must not fail: " + e.getMessage());
        }

        assertFalse("Expected <" + anyObject.hashCode() + "> to be unequal to <" + anyOtherObject.hashCode() +">",
                anyObject.hashCode() == anyOtherObject.hashCode());

        assertFalse("Expected <" + anyObject + "> to be unequal to <" + anyOtherObject +">",
                anyObject.equals(anyOtherObject));
    }
}