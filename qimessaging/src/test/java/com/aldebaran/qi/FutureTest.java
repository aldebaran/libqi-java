/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aldebaran.qi.ServiceDirectory;
import com.aldebaran.qi.Session;
import com.aldebaran.qi.ReplyService;
import com.aldebaran.qi.AnyObject;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for QiMessaging java bindings.
 */
public class FutureTest
{
  public AnyObject           proxy = null;
  public AnyObject           obj = null;
  public Session          s = null;
  public Session          client = null;
  public ServiceDirectory sd = null;
  public boolean          onSuccessCalled = false;
  public boolean          onCompleteCalled = false;

  @Before
  public void setUp() throws Exception
  {
    System.out.println("Setup...");
    onSuccessCalled = false;
    onCompleteCalled = false;
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
    ob.advertiseMethod("longReply::s(s)", reply, "Sleep 2s, then return given argument + 'bim !'");

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
    System.out.println("teardown...");
    s.close();
    client.close();

    s = null;
    client = null;
    sd = null;
  }

  @Test
  public void testThenSuccess()
  {
    try
    {
      int value = client.service("serviceTest").then(new QiFunction<Integer, AnyObject>() {
        @Override
        public Future<Integer> execute(Future<AnyObject> arg)
        {
          return arg.getValue().call("answer", 42);
        }
      }).get();
      // answer adds 1 to the value
      assertEquals(42 + 1, value);
    } catch (Exception e)
    {
      fail("get() must not fail");
    }
  }

  @Test
  public void testThenFailure()
  {
    try
    {
      client.service("nonExistant").then(new QiFunction<AnyObject, AnyObject>() {
        @Override
        public Future<AnyObject> execute(Future<AnyObject> arg)
        {
          try {
            arg.get();
            fail("get() must fail, the service does not exist");
          } catch (Exception e)
          {
            // expected exception
          }
          return client.service("serviceTest");
        }
      }).get();
    } catch (Exception e)
    {
      fail("get() must not fail, the second future should succeed");
    }
  }

  @Test
  public void testAndThenSuccess()
  {
    try
    {
      int value = client.service("serviceTest").andThen(new QiFunction<Integer, AnyObject>() {
        @Override
        public Future<Integer> execute(Future<AnyObject> arg)
        {
          return arg.getValue().call("answer", 42);
        }
      }).get();
      // answer adds 1 to the value
      assertEquals(42 + 1, value);
    } catch (Exception e)
    {
      fail("get() must not fail");
    }
  }

  @Test
  public void testAndThenFailure()
  {
    try
    {
      client.service("nonExistant").andThen(new QiFunction<Void, AnyObject>() {
        @Override
        public Future<Void> execute(Future<AnyObject> arg)
        {
          fail("The first future has failed, this code should never be called");
          return null;
        }
      }).get();
      fail("get() must fail");
    } catch (Exception e)
    {
      // expected exception
    }
  }

  @Test
  public void testImmediateValue()
  {
    try
    {
      int value = Future.of(42).get();
      assertEquals(42, value);
    } catch (Exception e)
    {
      fail("get() must not fail");
    }
  }

  @Test
  public void testImmediateNullValue()
  {
    try
    {
      Object value = Future.of(null).get();
      assertNull(value);
    } catch (Exception e)
    {
      fail("get() must not fail");
    }
  }

  @Test
  public void testConnectCallbackSuccess()
  {
    Future<String> future = proxy.call("longReply", "plaf");

    // the callback may be called from another thread
    final AtomicBoolean finished = new AtomicBoolean();

    future.connect(new Future.Callback<String>()
    {
      @Override
      public void onFinished(Future<String> future)
      {
        finished.set(true);
        try
        {
          future.get();
        } catch (Exception e)
        {
          fail("get() must not fail");
        }
      }
    });

    try
    {
      future.get();
    } catch (Exception e)
    {
      fail("get() must not fail");
    }

    assertTrue(finished.get());
  }

  @Test
  public void testConnectCallbackFailure()
  {
    Future<Void> future = proxy.call("throwUp");

    // the callback may be called from another thread
    final AtomicBoolean finished = new AtomicBoolean();

    future.connect(new Future.Callback<Void>()
    {
      @Override
      public void onFinished(Future<Void> future)
      {
        finished.set(true);
        try
        {
          future.get();
          fail("get() must fail");
        } catch (Exception e)
        {
          // expected exception
        }
      }
    });

    try
    {
      future.get();
      fail("get() must fail");
    } catch (Exception e)
    {
      // expected exception
    }

    assertTrue(finished.get());
  }

  @Test
  public void testCallback()
  {
    AnyObject proxy = null;
    Future<String> fut = null;


    // Get a proxy to serviceTest
    try
    {
      proxy = client.service("serviceTest").get();
    } catch (Exception e1)
    {
      fail("Cannot get serviceTest :" + e1.getMessage());
    }

    // Call a 2s long function
    try
    {
      fut = proxy.call("longReply", "plaf");
      fut.addCallback(new Callback<String>() {

        public void onSuccess(Future<String> future, Object[] args)
        {
          onSuccessCalled = true;
          assertEquals(1, args[0]);
          assertEquals(2, args[1]);
        }

        public void onFailure(Future<String> future, Object[] args)
        {
          fail("onFailure must not be called");
        }

        public void onComplete(Future<String> future, Object[] args)
        {
          onCompleteCalled = true;
          assertEquals(1, args[0]);
          assertEquals(2, args[1]);
        }
      }, 1, 2);
    } catch (CallError e)
    {
      fail("Error calling answer function : " + e.getMessage());
    }

    try
    {
      fut.get();
    } catch (Exception e)
    {
      fail("fut.get() must not fail");
    }
    assertTrue(onSuccessCalled);
    assertTrue(onCompleteCalled);
  }

  @Test
  public void testLongCall()
  {
    Future<String> fut = null;

    // Call a 2s long function
    try
    {
      fut = proxy.call("longReply", "plaf");
    } catch (CallError e)
    {
      fail("Error calling answer function : " + e.getMessage());
    }

    // Wait for call to finish.
    int count = 0;
    while (fut.isDone() == false)
    {
      count++;
      try
      {
        Thread.sleep(100);
      } catch (InterruptedException e) {}
    }

    assertTrue("isDone() must return false at least 3 times (" + count + ")", count > 3);

    // Get and print result
    try
    {
      String result = fut.get();
      assertEquals("plafbim !", result);
    } catch (Exception e)
    {
      fail("Call has been interrupted ("+ e.getMessage() + ")");
    }
  }

  @Test
  public void testGetTimeout()
  {
    System.out.println("testGetTimeout...");
    Future<String> fut = null;

    // Call a 2s long function
    try
    {
      fut = proxy.call("longReply", "plaf");
    } catch (CallError e)
    {
      System.out.println("Error calling answer function : " + e.getMessage());
      return;
    }

    boolean hasTimeout = false;
    try
    {
      fut.get(200, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e)
    {
      hasTimeout = true;
    } catch (Exception e) {}

    assertTrue("Future.get() must timeout", hasTimeout);

    try
    {
      String ret = fut.get();
      assertEquals("plafbim !", ret);
    } catch (Exception e1)
    {
      fail("InterruptedException must not be thrown");
    }
  }

  @Test
  public void testGetTimeoutSuccess()
  {
   System.out.println("testGetTimeoutSuccess...");
    Future<String> fut = null;

    // Call a 2s long function
    try
    {
      fut = proxy.call("longReply", "plaf");
    } catch (CallError e)
    {
      System.out.println("Error calling answer function : " + e.getMessage());
      return;
    }

    String ret = null;
    try
    {
      ret = fut.get(3, TimeUnit.SECONDS);
      System.out.println("Got ret");
    } catch (TimeoutException e)
    {
      fail("Call must not timeout");
    } catch (Exception e1)
    {
      fail("InterruptedException must not be thrown");
    }

    assertEquals("plafbim !", ret);
  }

  @Test
  public void testTimeout() throws InterruptedException, CallError
  {
    System.out.println("testTimeout...");
    Future<Void> fut = null;
    try
    {
      fut = proxy.call("longReply", "plaf");
      fut.sync(150, TimeUnit.MILLISECONDS);
    } catch (Exception e)
    {
    }

    assertFalse(fut.isDone());
    fut.sync(150, TimeUnit.MILLISECONDS);
    assertFalse(fut.isDone());
    fut.sync(500, TimeUnit.SECONDS);
    assertTrue(fut.isDone());
    assertEquals("plafbim !", fut.get());
  }

  @Test
  public void testSessionTimeout()
  {
    Session test = new Session();
    Future<Void> fut = null;

    try {
      // Arbitrary chosen non valid address
      fut = test.connect("tcp://198.18.0.1:9559");
      fut.sync(1, TimeUnit.SECONDS);
    } catch (Exception e) {
      fail("No way! : " + e.getMessage());
    }

    assertFalse(fut.isDone());
    assertFalse(test.isConnected());
  }
}
