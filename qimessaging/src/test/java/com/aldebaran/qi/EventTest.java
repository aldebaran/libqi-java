/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EventTest
{
  private boolean         callbackCalled = false;
  private int             callbackParam = 0;
  public AnyObject           proxy = null;
  public AnyObject           obj = null;
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

    // Connect session to Service Directory
    s.connect(url).sync();

    // Register service as serviceTest
    obj = ob.object();
    assertTrue("Service must be registered", s.registerService("serviceTest", obj) > 0);

    // Connect client session to service directory
    client.connect(url).sync();

    // Get a proxy to serviceTest
    proxy = client.service("serviceTest").get();
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
  }

  @Test
  public void testEvent() throws InterruptedException
  {

    @SuppressWarnings("unused")
    Object callback = new Object() {
      public void fireCallback(Integer i)
      {
        callbackCalled = true;
        callbackParam = i.intValue();
      }
    };

    long sid = 0;
    try {
      sid = proxy.connect("fire::(i)", "fireCallback::(i)", callback);
    } catch (Exception e) {
      fail("Connect to event must succeed : " + e.getMessage());
    }
    obj.post("fire", 42);

    Thread.sleep(100); // Give time for callback to be called.
    assertTrue("Event callback must have been called ", callbackCalled);
    assertTrue("Parameter value must be 42 (" + callbackParam + ")", callbackParam == 42);

    proxy.disconnect(sid);
    callbackCalled = false;
    obj.post("fire", 42);
    Thread.sleep(100);
    assertTrue("Event callback not called ", ! callbackCalled);
  }

  public void testCallback(String s)
  {
    callbackCalled = true;
  }

  @Test
  public void testSessionOnDisconnected() throws InterruptedException
  {
    callbackCalled = false;
    client.onDisconnected("testCallback", this);
    client.close();
    Thread.sleep(100);
    assertTrue(callbackCalled);
  }

  @Test
  public void testSignal() throws InterruptedException
  {
    final AtomicInteger value = new AtomicInteger();
    QiSignalConnection connection = proxy.connect("fire", new QiSignalListener()
    {
      @Override
      public void onSignalReceived(Object... args)
      {
        int v = (Integer) args[0];
        value.set(v);
      }
    });
    connection.waitForDone();
    obj.post("fire", 42);
    Thread.sleep(100);
    assertEquals(42, value.get());
    obj.post("fire", 99);
    Thread.sleep(100);
    assertEquals(99, value.get());
    connection.disconnect().sync();
    obj.post("fire", 12);
    Thread.sleep(100);
    assertEquals(99, value.get());
  }

  @Test
  public void testSessionConnectionListener() throws ExecutionException, InterruptedException
  {
    Session session = new Session();
    final AtomicBoolean connectedCalled = new AtomicBoolean();
    final AtomicBoolean disconnectedCalled = new AtomicBoolean();
    session.addConnectionListener(new Session.ConnectionListener()
    {
      @Override
      public void onConnected()
      {
        connectedCalled.set(true);
      }

      @Override
      public void onDisconnected(String reason)
      {
        disconnectedCalled.set(true);
      }
    });
    session.connect(sd.listenUrl()).get();
    session.close();
    Thread.sleep(100);
    assertTrue(connectedCalled.get());
    assertTrue(disconnectedCalled.get());
  }
}
