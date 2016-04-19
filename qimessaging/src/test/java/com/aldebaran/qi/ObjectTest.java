/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class ObjectTest
{
  public AnyObject     proxy = null;
  public AnyObject     proxyts = null;
  public AnyObject     obj = null;
  public AnyObject     objts = null;
  public Session          s = null;
  public Session          client = null;
  public ServiceDirectory sd = null;

  @Before
  public void setUp() throws Exception
  {
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
    ob.advertiseMethod("setStored::v(i)", reply, "Set stored value");
    ob.advertiseMethod("waitAndAddToStored::i(ii)", reply, "Wait given time, and return stored + val");

    QiService replyts = new ReplyService();
    DynamicObjectBuilder obts = new DynamicObjectBuilder();
    obts.advertiseMethod("setStored::v(i)", replyts, "Set stored value");
    obts.advertiseMethod("waitAndAddToStored::i(ii)", replyts, "Wait given time, and return stored + val");
    obts.advertiseMethod("throwUp::v()", replyts, "Throws");
    obts.setThreadingModel(DynamicObjectBuilder.ObjectThreadingModel.MultiThread);

    // Connect session to Service Directory
    s.connect(url).sync();

    // Register service as serviceTest
    obj = ob.object();
    objts = obts.object();
    assertTrue("Service must be registered", s.registerService("serviceTest", obj) > 0);
    assertTrue("Service must be registered", s.registerService("serviceTestTs", objts) > 0);

    // Connect client session to service directory
    client.connect(url).sync();

    // Get a proxy to serviceTest
    proxy = client.service("serviceTest").get();
    proxyts = client.service("serviceTestTs").get();
    assertNotNull(proxy);
    assertNotNull(proxyts);
  }

  @After
  public void tearDown()
  {
    obj = null;
    proxy = null;

    s.close();
    client.close();

    s = null;
    client = null;
    sd = null;
  }

  @Test
  public void singleThread() throws Exception
  {
    Future<Integer> v0;
    v0 = proxy.<Integer>call("waitAndAddToStored", 500, 0);
    Thread.sleep(10);
    Future<Void> v1 = proxy.<Void>call("setStored", 42);
    assertEquals(v0.get(), new Integer(0));
  }
  @Test
  public void multiThread() throws Exception
  {
    Future<Integer> v0 = proxyts.<Integer>call("waitAndAddToStored", 500, 0);
    Thread.sleep(10);
    Future<Void> v1 = proxyts.<Void>call("setStored", 42);
    assertEquals(v0.get(), new Integer(42));
  }

  @Test
  public void callThrow() throws Exception
  {
    Future<Integer> v0 = proxyts.<Integer>call("throwUp");
    assertTrue(v0.hasError());
    assertEquals(v0.getErrorMessage(), "I has faild");
  }

  @Test
  public void getErrorOnSuccess() throws Exception
  {
    Future<Void> v0 = proxyts.<Void>call("setStored", 18);
    assertNull(v0.getError());
    assertFalse(v0.hasError());
  }

  @Test
  public void getObject()
  {
    boolean ok;
    AnyObject ro = null;

    try
    {
      ro = proxy.<AnyObject>call("createObject").get();
    } catch (Exception e)
    {
      fail("Call must not fail: " + e.getMessage());
    }

    assertNotNull(ro);
    try {
      assertEquals("foo", ro.<String>property("name").get());
    } catch (Exception e1) {
      fail("Property must not fail");
    }

    Map<Object, Object> settings = new HashMap<Object, Object>();
    settings.put("foo", true);
    settings.put("bar", "This is bar");
    try {
      ro.setProperty("settings", settings);
    } catch (Exception e) {
      fail("Call must succeed: " + e.getMessage());
    }
    Map<Object, Object> readSettings = null;
    try {
      readSettings = ro.<Map<Object, Object>>property("settings").get();
    } catch (ExecutionException e) {
      fail("Execution must not fail: " + e.getMessage());
    }
    assertEquals(readSettings.get("foo"), true);
    assertEquals(readSettings.get("bar"), "This is bar");

    String ret = null;
    try {
      ret = ro.<String>call("answer").get();
    } catch (Exception e) {
      fail("Call must succeed : " + e.getMessage());
    }
    assertEquals("42 !", ret);

    ok = false;
    try {
        ret = ro.<String>call("add", 42).get();
      } catch (Exception e) {
        ok = true;
        String expected = "Arguments types did not match for add:\n  Candidate:\n  add::(iii) (1)\n";
        System.out.println(e.getMessage());
        System.out.println(expected);
        assertTrue(e.getMessage().contains("did not match"));
      }
    assertTrue(ok);

    ok = false;
    try {
        ret = ro.<String>call("add", "42", 42, 42).get();
      } catch (ExecutionException e) {
        ok = true;
        String expected = "cannot convert parameters from (sii) to (iii)";
        System.out.println(e.getMessage());
        System.out.println(expected);
        assertEquals(expected, e.getCause().getMessage());
      }
    assertTrue(ok);

    ok = false;
    try {
        ret = ro.<String>call("addFoo").get();
      } catch (Exception e) {
        ok = true;
        String expected = "Can't find method: addFoo\n";
        System.out.println(e.getMessage());
        System.out.println(expected);
        assertTrue(e.getMessage().contains("Can't find method"));
      }
    assertTrue(ok);
  }
}
