package com.aldebaran.qi;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.aldebaran.qi.ServiceDirectory;
import com.aldebaran.qi.Session;
import com.aldebaran.qi.ReplyService;
import com.aldebaran.qi.AnyObject;
import com.aldebaran.qi.Application;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for QiMessaging java bindings.
 */
public class TypeTest
{
  public AnyObject           proxy = null;
  public AnyObject           obj = null;
  public Session          s = null;
  public Session          client = null;
  public ServiceDirectory sd = null;

  @Before
  public void setUp() throws Exception
  {
    //Application.setLogCategory("qimessaging.jni", 6);
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
    ob.advertiseMethod("generic::b(m)", reply, "Take a value as argument");

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
    sd.close();

    s = null;
    client = null;
    sd = null;
  }

  /**
   * Test String conversion
   */
  @Test
  public void testString()
  {
    String ret = null;
    try {
      ret = proxy.<String>call("reply", "plaf").get();
    }
    catch (Exception e)
    {
      fail("Call Error must not be thrown : " + e.getMessage());
    }

    assertEquals("plafbim !", ret);

    try {
      ret = proxy.<String>call("answer").get();
    }
    catch (Exception e)
    {
      fail("Call Error must not be thrown : " + e.getMessage());
    }

    assertEquals("42 !", ret);
  }

  /**
   * Test Integer conversion
   */
  @Test
  public void testInt()
  {
    Integer ret = null;
    try {
      ret = proxy.<Integer>call("answer", 41).get();
    }
    catch (Exception e)
    {
      fail("Call Error must not be thrown : " + e.getMessage());
    }

    assertEquals(42, ret.intValue());
  }

  /**
   * Test Float conversion
   */
  @Test
  public void testFloat()
  {
    Float ret = null;
    try {
      ret = proxy.<Float>call("answerFloat", 41.2f).get();
    }
    catch (Exception e)
    {
      fail("Call Error must not be thrown : " + e.getMessage());
    }

    assertEquals(42.2f, ret.floatValue(), 0.1f);
  }

  /**
   * Test Boolean conversion
   */
  @Test
  public void testBoolean()
  {
    Boolean ret = null;
    try {
      ret = proxy.<Boolean>call("answerBool", false).get();
    }
    catch (Exception e)
    {
      fail("Call Error must not be thrown : " + e.getMessage());
    }

    assertTrue("Result must be true", ret);
  }

  /**
   * Test Map conversion
   */
  @Test
  public void testEmptyMap()
  {
    Map<Integer, Boolean> args = new Hashtable<Integer, Boolean>();

    Map<Integer, Boolean> ret = null;
    try {
      ret = proxy.<Hashtable<Integer, Boolean> >call("abacus", args).get();
    }
    catch (Exception e)
    {
      fail("Call Error must not be thrown : " + e.getMessage());
    }

    assertTrue("Result must be empty", ret.size() == 0);
  }

  /**
   * Test Map conversion
   */
  @Test
  public void testIntegerBooleanMap()
  {
    Map<Integer, Boolean> args = new Hashtable<Integer, Boolean>();
    args.put(4, true);
    args.put(3, false);
    args.put(2, false);
    args.put(1, true);

    Map<Integer, Boolean> ret = null;
    try {
      ret = proxy.<Hashtable<Integer, Boolean> >call("abacus", args).get();
    }
    catch (Exception e)
    {
      fail("Call Error must not be thrown : " + e.getMessage());
    }

    assertFalse("Result must be false", ret.get(1));
    assertTrue("Result must be true", ret.get(2));
    assertTrue("Result must be true", ret.get(3));
    assertFalse("Result must be false", ret.get(4));
  }

  /**
   * Test List conversion
   */
  @Test
  public void testEmptyList()
  {
    List<Float> args = new ArrayList<Float>();

    List<Float> ret = null;
    try {
      ret = proxy.<ArrayList<Float> >call("echoFloatList", args).get();
    }
    catch (Exception e)
    {
      fail("Call Error must not be thrown : " + e.getMessage());
    }

    assertTrue("Result must be empty", ret.size() == 0);
  }

  /**
   * Test List conversion
   */
  @Test
  public void testIntegerList()
  {
    List<Float> args = new ArrayList<Float>();
    args.add(13.3f);
    args.add(1342.3f);
    args.add(13.4f);
    args.add(1.0f);
    args.add(0.1f);

    List<Float> ret = null;
    try {
      ret = proxy.<ArrayList<Float> >call("echoFloatList", args).get();
    }
    catch (Exception e)
    {
      fail("Call Error must not be thrown : " + e.getMessage());
    }

    assertEquals(args, ret);
  }

  @Test
  public void testValue()
  {
    String str = "hello world";
    Boolean ret = null;

    try {
      ret = proxy.<Boolean>call("generic", str).get();
    }
    catch (Exception e)
    {
      fail("Call Error must not be thrown : " + e.getMessage());
    }

    assertTrue(ret);
  }

  @Test
  public void testConvert()
  {
    Object o = AnyObject.decodeJSON("1");
    System.out.println(o.getClass().getName());
    assertTrue(o instanceof java.lang.Integer);
    assertTrue(((Integer)o).equals(1));
    String str = AnyObject.encodeJSON(o);
    assertEquals(str, "1");

    o = AnyObject.decodeJSON("1.5");
    System.out.println(o.getClass().getName());
    System.out.println(o.toString());
    assertTrue(o instanceof java.lang.Float);
    assertTrue(((Float)o).equals(1.5f));
    str = AnyObject.encodeJSON(o);
    assertEquals(str, "1.5");

    o = AnyObject.decodeJSON("\"foo\"");
    assertTrue(o instanceof java.lang.String);
    assertTrue(((String)o).equals("foo"));
    str = AnyObject.encodeJSON(o);
    assertEquals(str, "\"foo\"");

    System.gc(); // just for fun

    o = AnyObject.decodeJSON("[1, 2, 3]");
    assertTrue(o instanceof List);
    List l = (List)o;
    assertEquals(l.size(), 3);
    assertEquals(l.get(0), 1);
    assertEquals(l.get(2), 3);
    str = AnyObject.encodeJSON(o);
    // be leniant on non-significant formatting
    assertEquals(str.replace(" ",""), "[1,2,3]");
  }
  @Test
  public void testConvertBig()
  {
    System.out.println("big test started");
    String mega = "[";
    for (int i=0; i<1000; i++)
    {
      mega += Integer.toString(i) + ",";
      //System.out.println("megamega..." + mega);
    }
    mega += "1]";
    System.out.println("big test decoding");
    Object o = AnyObject.decodeJSON(mega);
    System.out.println("big test decoded");
    assertTrue(o instanceof List);
    List l = (List)o;
    assertEquals(l.size(), 1001);
    assertEquals(l.get(100), 100);
    String str = AnyObject.encodeJSON(o);
    assertEquals(str.replace(" ",""), mega);
    System.out.println("big test finished");
  }
}
