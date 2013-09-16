package com.aldebaran.qimessaging;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ObjectTest
{
  public Object     proxy = null;
  public Object     obj = null;
  public Session          s = null;
  public Session          client = null;
  public ServiceDirectory sd = null;
  public Application app = null;

  @Before
  public void setUp() throws Exception
  {
    app = new Application(null);
    sd = new ServiceDirectory();
    s = new Session();
    client = new Session();

    // Get Service directory listening url.
    String url = sd.listenUrl();

    // Create new QiMessaging generic object
    DynamicObjectBuilder ob = new DynamicObjectBuilder();

    // Get instance of ReplyService
    QimessagingService reply = new ReplyService();

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
    assertTrue("Service must be registered", s.registerService("serviceTest", obj) > 0);

    // Connect client session to service directory
    client.connect(url).sync();

    // Get a proxy to serviceTest
    proxy = client.service("serviceTest");
    assertNotNull(proxy);
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
    app.stop();
    app = null;
  }

  @Test
  public void getObject()
  {
    Object ro = null;

    try
    {
      ro = proxy.<Object>call("createObject").get();
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

    String ret = null;
    try {
      ret = ro.<String>call("answer").get();
    } catch (Exception e) {
      fail("Call must succeed : " + e.getMessage());
    }
    assertEquals("42 !", ret);

    try {
        ret = ro.<String>call("add", 42).get();
      } catch (Exception e) {
        String expected = "Arguments types did not match for add::(i):\n  Candidate:\n  add::(iii) (1)\n";
        assertEquals(expected, e.getMessage());
      }

    try {
        ret = ro.<String>call("add", "42", 42, 42).get();
      } catch (Exception e) {
        String expected = "remote call: failed to convert argument 0 from m to i";
        assertEquals(expected, e.getMessage());
      }

    try {
        ret = ro.<String>call("addFoo").get();
      } catch (Exception e) {
          String expected = "Can't find method: addFoo::()<>\n";
	      assertEquals(expected, e.getMessage());
      }
  }
}
