/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
    ob.advertiseMethod("throwUp::v()", reply, "Throws");

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
      int value = client.service("serviceTest").then(new FutureFunction<Integer, AnyObject>()
      {
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
      client.service("nonExistant").then(new FutureFunction<AnyObject, AnyObject>()
      {
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
  public void testThenReturnNull()
  {
    try
    {
      Void result = client.service("serviceTest").then(new FutureFunction<Void, AnyObject>()
      {
        @Override
        public Future<Void> execute(Future<AnyObject> arg)
        {
          return null;
        }
      }).get();
      assertNull(result);
    } catch (Exception e)
    {
      fail("get() must not fail");
    }
  }

  @Test
  public void testAndThenSuccess()
  {
    try
    {
      int value = client.service("serviceTest").andThen(new FutureFunction<Integer, AnyObject>()
      {
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
      client.service("nonExistant").andThen(new FutureFunction<Void, AnyObject>()
      {
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
  public void testAndThenReturnNull()
  {
    try
    {
      Void result = client.service("serviceTest").andThen(new FutureFunction<Void, AnyObject>()
      {
        @Override
        public Future<Void> execute(Future<AnyObject> arg)
        {
          return null;
        }
      }).get();
      assertNull(result);
    } catch (Exception e)
    {
      fail("get() must not fail");
    }
  }

  @Test
  public void testQiFunctionException()
  {
    try
    {
      client.service("serviceTest").andThen(new FutureFunction<Void, AnyObject>()
      {
        @Override
        public Future<Void> execute(Future<AnyObject> arg)
        {
          throw new RuntimeException("something went wrong (fake)");
        }
      }).get();
      fail("get() must fail");
    } catch (ExecutionException e)
    {
      // expected exception
      QiException cause = (QiException) e.getCause();
      assertTrue("Exception must contain the error from the future",
          cause.getMessage().contains("something went wrong (fake)"));
    }
  }

  @Test
  public void testThenAdapterSuccess()
  {
    try
    {
      int value = client.service("serviceTest").then(new QiFunction<Integer, AnyObject>()
      {
        @Override
        public Future<Integer> onResult(AnyObject service)
        {
          return service.call("answer", 42);
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
  public void testThenAdapterFailure()
  {
    final AtomicBoolean onErrorCalled = new AtomicBoolean();
    try
    {
      client.service("nonExistant").then(new QiFunction<AnyObject, AnyObject>()
      {
        @Override
        public Future<AnyObject> onResult(AnyObject service)
        {
          fail("onResult() must not be called, the service does not exist");
          return null;
        }

        @Override
        public Future<AnyObject> onError(Throwable error) throws ExecutionException
        {
          onErrorCalled.set(true);
          return client.service("serviceTest");
        }
      }).get();
    } catch (Exception e)
    {
      fail("get() must not fail, the second future should succeed");
    }
    assertTrue("onError() must be called", onErrorCalled.get());
  }

  @Test
  public void testAndThenAdapterSuccess()
  {
    try
    {
      int value = client.service("serviceTest").andThen(new QiFunction<Integer, AnyObject>()
      {
        @Override
        public Future<Integer> onResult(AnyObject service)
        {
          return service.call("answer", 42);
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
  public void testAndThenAdapterFailure()
  {
    try
    {
      client.service("nonExistant").andThen(new QiCallback<AnyObject>()
      {
        @Override
        public void onResult(AnyObject service)
        {
          fail("The first future has failed, this code should never be called");
        }
      }).get();
      fail("get() must fail");
    } catch (Exception e)
    {
      // expected exception
    }
  }

  @Test
  public void testThenVoidFunction() throws Exception {
    final AtomicBoolean called = new AtomicBoolean();
    client.service("serviceTest").andThen(new QiCallback<AnyObject>()
    {
      @Override
      public void onResult(AnyObject service)
      {
        called.set(true);
      }
    }).get();
    // answer adds 1 to the value
    assertTrue(called.get());
  }

  public static boolean isCallbackExecutedOnSameThread(FutureCallbackType promiseType, FutureCallbackType thenType) throws InterruptedException
  {
    Promise<Void> promise = new Promise<Void>(promiseType);
    promise.setValue(null);
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    final AtomicLong callbackThreadId = new AtomicLong();
    promise.getFuture().andThen(new QiCallback<Void>()
    {
      @Override
      public void onResult(Void result)
      {
        callbackThreadId.set(Thread.currentThread().getId());
        countDownLatch.countDown();
      }
    }, thenType);
    countDownLatch.await();
    return Thread.currentThread().getId() == callbackThreadId.get();
  }

  private static void expectSync(FutureCallbackType promiseType, FutureCallbackType thenType) throws InterruptedException {
    assertTrue(isCallbackExecutedOnSameThread(promiseType, thenType));
  }

  private static void expectAsync(FutureCallbackType promiseType, FutureCallbackType thenType) throws InterruptedException {
    assertFalse(isCallbackExecutedOnSameThread(promiseType, thenType));
  }

  @Test
  public void testFutureCallbackTypes() throws InterruptedException
  {
    // the type of "then" is Sync, so the resulting type is always Sync
    expectSync(FutureCallbackType.Sync, FutureCallbackType.Sync);
    expectSync(FutureCallbackType.Async, FutureCallbackType.Sync);
    expectSync(FutureCallbackType.Auto, FutureCallbackType.Sync);

    // the type of "then" is Async, so the resulting type is always Async
    expectAsync(FutureCallbackType.Sync, FutureCallbackType.Async);
    expectAsync(FutureCallbackType.Async, FutureCallbackType.Async);
    expectAsync(FutureCallbackType.Auto, FutureCallbackType.Async);

    // the type of "then" is Auto, so the resulting type is the promise type
    expectSync(FutureCallbackType.Sync, FutureCallbackType.Auto);
    expectAsync(FutureCallbackType.Async, FutureCallbackType.Auto);
  }

  @Test
  public void testUnknownType() throws ExecutionException
  {
    class X
    {
      int value;

      X(int value)
      {
        this.value = value;
      }
    }

    Promise<X> promise = new Promise<X>();
    promise.setValue(new X(42));
    Future<X> future = promise.getFuture();
    X x = future.andThen(new QiFunction<X, X>()

    {
      @Override
      public Future<X> onResult(X x) throws Exception
      {
        return Future.of(new X(x.value + 1));
      }
    }).get();
    assertEquals(43, x.value);
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
      fut.addCallback(new Callback<String>()
      {

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
    } catch (DynamicCallException e)
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
    } catch (DynamicCallException e)
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
    } catch (DynamicCallException e)
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
    } catch (DynamicCallException e)
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
  public void testTimeout() throws ExecutionException
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

  public void testExecutionException()
  {
    try {
      proxy.call("throwUp").get();
    } catch (ExecutionException e) {
      assertTrue("ExecutionException cause should extend QiException", e.getCause() instanceof QiException);
      return;
    }
    fail("Must have thrown ExecutionException");
  }

  @Test(expected=CancellationException.class)
  public void testCancelMatchesJavaFutureSemantics() throws ExecutionException
  {
    Future<Void> future = proxy.call("longReply", "plaf");
    future.cancel(false);
    future.get();
  }

  private static class AsyncWait implements Runnable
  {
    enum Type
    {
      VALUE, ERROR, CANCEL
    }

    private Promise<Long> promise;
    private long milliseconds;
    private Type finishType;

    AsyncWait(Promise<Long> promise, long milliseconds, Type finishType)
    {
      this.promise = promise;
      this.milliseconds = milliseconds;
      this.finishType = finishType;
    }

    @Override
    public void run()
    {
      try
      {
        Thread.sleep(milliseconds);
      } catch (InterruptedException e)
      {
        throw new RuntimeException(e);
      }
      switch (finishType)
      {
      case VALUE:
        promise.setValue(milliseconds);
        break;
      case ERROR:
        promise.setError("Mock error after " + milliseconds);
        break;
      case CANCEL:
        promise.setCancelled();
      }
    }
  }

  @Test
  public void testWaitAll() throws ExecutionException, TimeoutException {
    Promise<Long> p1 = new Promise<Long>();
    Promise<Long> p2 = new Promise<Long>();
    Promise<Long> p3 = new Promise<Long>();
    new Thread(new AsyncWait(p1, 100, AsyncWait.Type.VALUE)).start();
    new Thread(new AsyncWait(p2, 200, AsyncWait.Type.VALUE)).start();
    p3.setValue(42L);
    Future<Long> f1 = p1.getFuture();
    Future<Long> f2 = p2.getFuture();
    Future<Long> f3 = p3.getFuture();
    Future.waitAll(f1, f2, f3).get();
    assertEquals(100, (long) f1.get(0, TimeUnit.SECONDS));
    assertEquals(200, (long) f2.get(0, TimeUnit.SECONDS));
    assertEquals(42, (long) f3.get(0, TimeUnit.SECONDS));
  }

  @Test(expected=ExecutionException.class)
  public void testWaitAllWithError() throws ExecutionException, TimeoutException {
    Promise<Long> p1 = new Promise<Long>();
    Promise<Long> p2 = new Promise<Long>();
    new Thread(new AsyncWait(p1, 100, AsyncWait.Type.ERROR)).start();
    new Thread(new AsyncWait(p2, 200, AsyncWait.Type.VALUE)).start();
    Future<Long> f1 = p1.getFuture();
    Future<Long> f2 = p2.getFuture();
    Future.waitAll(f1, f2).get();
  }

  @Test(expected=CancellationException.class)
  public void testWaitAllWithCancellation() throws ExecutionException, TimeoutException {
    Promise<Long> p1 = new Promise<Long>();
    Promise<Long> p2 = new Promise<Long>();
    new Thread(new AsyncWait(p1, 100, AsyncWait.Type.CANCEL)).start();
    new Thread(new AsyncWait(p2, 200, AsyncWait.Type.VALUE)).start();
    Future<Long> f1 = p1.getFuture();
    Future<Long> f2 = p2.getFuture();
    Future.waitAll(f1, f2).get();
  }

  @Test
  public void testWaitAllEmpty() throws ExecutionException, TimeoutException {
    Future.waitAll().get(1, TimeUnit.SECONDS);
  }
}
