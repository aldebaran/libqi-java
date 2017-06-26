/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Integration test for QiMessaging java bindings.
 */
public class IntegrationTest {
    public AnyObject proxy = null;
    public AnyObject obj = null;
    public Session s = null;
    public Session client = null;
    public ServiceDirectory sd = null;
    public int idx = 0;

    @Before
    public void setUp() throws Exception {
        sd = new ServiceDirectory();
        s = new Session();
        client = new Session();

        // Get Service directory listening url.
        String url = sd.listenUrl();

        // Create new QiMessaging generic object
        DynamicObjectBuilder ob = new DynamicObjectBuilder();

        // Get instance of ReplyService
        QiService reply = new ReplyService();

        // Register event 'Fire'
        ob.advertiseSignal("fire::(i)");
        ob.advertiseMethod("reply::s(s)", reply, "Concatenate given argument with 'bim !'");
        ob.advertiseMethod("answer::s()", reply, "Return given argument");
        ob.advertiseMethod("add::i(iii)", reply, "Return sum of arguments");
        ob.advertiseMethod("info::(sib)(sib)", reply, "Return a tuple containing given arguments");
        ob.advertiseMethod("answer::i(i)", reply, "Return given parameter plus 1");
        ob.advertiseMethod("answerFloat::f(f)", reply, "Return given parameter plus 1");
        ob.advertiseMethod("answerBool::b(b)", reply, "Flip given parameter and return it");
        ob.advertiseMethod("abacus::{ib}({ib})", reply, "Flip all booleans in map");
        ob.advertiseMethod("echoFloatList::[m]([f])", reply, "Return the exact same list");
        ob.advertiseMethod("createObject::o()", reply, "Return a test object");

        // Connect session to Service Directory
        s.connect(url).sync();

        // Register service as serviceTest
        obj = ob.object();
        idx = s.registerService("serviceTest", obj);
        assertTrue("Service must be registered", idx > 0);

        // Connect client session to service directory
        client.connect(url).sync();

        // Get a proxy to serviceTest
        proxy = client.service("serviceTest").get();
        assertNotNull(proxy);
    }

    @After
    public void tearDown() {
        s.unregisterService(idx);

        obj = null;
        proxy = null;

        s.close();
        client.close();

        s = null;
        client = null;
        sd = null;
    }

    /**
     * Create a dumb service binding famous reply::s(s) method,
     * then connect a client session to service directory,
     * get a proxy on 'serviceTest',
     * finally call 'reply::(s)' and check answer.
     */
    @Test
    public void testCallService() {
        String res = null;

        try {
            res = proxy.<String>call("reply::(s)", "plaf").get();
        } catch (Exception e) {
            fail("Call must succeed : " + e.getMessage());
        }

        assertNotNull(res);
        assertEquals("plafbim !", res);
    }

    /**
     * Create a dumb service binding famous reply::s(s) method,
     * then connect a client session to service directory,
     * get a proxy on 'serviceTest',
     * finally call 'reply' without signature and check answer.
     */
    @Test
    public void testCallReplyWithoutSignature() {
        String res = null;

        try {
            res = proxy.<String>call("reply", "plaf").get();
        } catch (Exception e) {
            fail("Call must succeed : " + e.getMessage());
        }

        assertNotNull(res);
        assertEquals("plafbim !", res);
    }

    /**
     * Test multiple arguments call
     */
    @Test
    public void testWithMultipleArguments() {
        Integer ret = null;
        try {
            ret = proxy.<Integer>call("add", 1, 21, 20).get();
        } catch (Exception e) {
            fail("Call Error must not be thrown : " + e.getMessage());
        }

        assertEquals(42, ret.intValue());
    }
}
