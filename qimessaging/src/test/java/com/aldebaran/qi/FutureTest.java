/*
 * * Copyright (C) 2015 SoftBank Robotics* See COPYING for the license
 */
package com.aldebaran.qi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for QiMessaging java bindings.
 */
public class FutureTest {
    public AnyObject proxy = null;
    public AnyObject obj = null;
    public Session s = null;
    public Session client = null;
    public ServiceDirectory sd = null;
    public boolean onSuccessCalled = false;
    public boolean onCompleteCalled = false;

    @Before
    public void setUp() throws Exception {
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
        ob.advertiseMethod("getFuture::s(s)", reply, "Returns a future");
        ob.advertiseMethod("getCancellableFuture::s(s)", reply, "Returns a cancellable future");

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
    public void tearDown() {
        System.out.println("teardown...");
        s.close();
        client.close();

        s = null;
        client = null;
        sd = null;
    }

    @Test
    public void testThenSuccess() {
        try {
            int value = client.service("serviceTest").map(new Function<Future<AnyObject>, Integer>() {
                @Override
                public Integer execute(Future<AnyObject> arg) throws ExecutionException {
                    Future<Integer> future = arg.getValue().call("answer", 42);
                    return future.get();
                }
            }).get();
            // answer adds 1 to the value
            assertEquals(42 + 1, value);
        }
        catch (Exception e) {
            fail("get() must not fail");
        }
    }

    @Test
    public void testThenFailure() {
        try {
            client.service("nonExistant").map(new Function<Future<AnyObject>, AnyObject>() {
                @Override
                public AnyObject execute(Future<AnyObject> arg) throws ExecutionException {
                    try {
                        arg.get();
                        fail("get() must fail, the service does not exist");
                    }
                    catch (Exception e) {
                        // expected exception
                    }
                    return client.service("serviceTest").get();
                }
            }).get();
        }
        catch (Exception e) {
            fail("get() must not fail, the second future should succeed");
        }
    }

    @Test
    public void testThenReturnNull() {
        try {
            Void result = client.service("serviceTest").then(new Consumer<Future<AnyObject>>() {
                @Override
                public void consume(Future<AnyObject> arg) {
                }
            }).get();
            assertNull(result);
        }
        catch (Exception e) {
            fail("get() must not fail");
        }
    }

    @Test
    public void testAndThenSuccess() {
        try {
            int value = client.service("serviceTest").andMap(new Function<AnyObject, Integer>() {
                @Override
                public Integer execute(AnyObject arg) throws ExecutionException {
                    return (Integer) arg.call("answer", 42).get();
                }
            }).get();
            // answer adds 1 to the value
            assertEquals(42 + 1, value);
        }
        catch (Exception e) {
            fail("get() must not fail");
        }
    }

    @Test
    public void testAndThenFailure() {
        try {
            client.service("nonExistant").andMap(new Function<AnyObject, Void>() {
                @Override
                public Void execute(AnyObject arg) {
                    fail("The first future has failed, this code should never be called");
                    return null;
                }
            }).get();
            fail("get() must fail");
        }
        catch (Exception e) {
            // expected exception
        }
    }

    @Test
    public void testAndThenReturnNull() {
        try {
            Void result = client.service("serviceTest").andThen(new Consumer<AnyObject>() {
                @Override
                public void consume(AnyObject value) throws Throwable {
                }

            }).get();
            assertNull(result);
        }
        catch (Exception e) {
            fail("get() must not fail");
        }
    }

    @Test
    public void testQiFunctionException() {
        try {
            client.service("serviceTest").andMap(new Function<AnyObject, Object>() {
                @Override
                public Object execute(AnyObject value) throws Throwable {
                    throw new RuntimeException("something went wrong (fake)");
                }
            }).get();
            fail("get() must fail");
        }
        catch (ExecutionException e) {
            // expected exception
            assertTrue("Exception must contain the error from the future",
                    e.getMessage().contains("something went wrong (fake)"));
        }
    }

    @Test
    public void testThenAdapterSuccess() {
        try {
            int value = client.service("serviceTest").map(new Function<Future<AnyObject>, Integer>() {
                @Override
                public Integer execute(Future<AnyObject> future) throws Throwable {
                    return (Integer) future.get().call("answer", 42).get();
                }
            }).get();
            // answer adds 1 to the value
            assertEquals(42 + 1, value);
        }
        catch (Exception e) {
            fail("get() must not fail");
        }
    }

    @Test
    public void testThenAdapterFailure() {
        final AtomicBoolean onErrorCalled = new AtomicBoolean();
        try {
            client.service("nonExistant").map(new Function<Future<AnyObject>, AnyObject>() {
                @Override
                public AnyObject execute(Future<AnyObject> future) throws Throwable {
                    if (future.hasError()) {
                        onErrorCalled.set(true);
                        return client.service("serviceTest").get();
                    }
                    fail("onResult() must not be called, the service does not exist");
                    return null;
                }
            }).get();
        }
        catch (Exception e) {
            fail("get() must not fail, the second future should succeed");
        }
        assertTrue("onError() must be called", onErrorCalled.get());
    }

    @Test
    public void testAndThenAdapterSuccess() {
        try {
            int value = client.service("serviceTest").andMap(new Function<AnyObject, Integer>() {
                @Override
                public Integer execute(AnyObject service) throws Throwable {
                    return (Integer) service.call("answer", 42).get();
                }
            }).get();
            // answer adds 1 to the value
            assertEquals(42 + 1, value);
        }
        catch (Exception e) {
            fail("get() must not fail");
        }
    }

    @Test
    public void testAndThenAdapterFailure() {
        try {
            client.service("nonExistant").andMap(new Function<AnyObject, Void>() {
                @Override
                public Void execute(AnyObject value) throws Throwable {
                    fail("The first future has failed, this code should never be called");
                    return null;
                }
            }).get();
            fail("get() must fail");
        }
        catch (Exception e) {
            // expected exception
        }
    }

    @Test
    public void testThenVoidFunction() throws Exception {
        final AtomicBoolean called = new AtomicBoolean();
        client.service("serviceTest").andMap(new Function<AnyObject, Object>() {
            @Override
            public Object execute(AnyObject value) throws Throwable {
                called.set(true);
                return null;
            }
        }).get();
        // answer adds 1 to the value
        assertTrue(called.get());
    }

    public static boolean isCallbackExecutedOnSameThread(FutureCallbackType promiseType) throws InterruptedException {
        Promise<Void> promise = new Promise<Void>(promiseType);
        promise.setValue(null);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicLong callbackThreadId = new AtomicLong();
        promise.getFuture().andMap(new Function<Void, Object>() {
            @Override
            public Object execute(Void value) throws Throwable {
                callbackThreadId.set(Thread.currentThread().getId());
                countDownLatch.countDown();
                return null;
            }

        });
        countDownLatch.await();
        return Thread.currentThread().getId() == callbackThreadId.get();
    }

    @Test
    public void testUnknownType() throws ExecutionException {
        class X {
            int value;

            X(int value) {
                this.value = value;
            }
        }

        Promise<X> promise = new Promise<X>();
        promise.setValue(new X(42));
        Future<X> future = promise.getFuture();
        X x = future.andMap(new Function<X, X>()

        {
            @Override
            public X execute(X x) throws Throwable {
                return new X(x.value + 1);
            }

        }).get();
        assertEquals(43, x.value);
    }

    @Test
    public void testImmediateValue() {
        try {
            int value = Future.of(42).get();
            assertEquals(42, value);
        }
        catch (Exception e) {
            fail("get() must not fail");
        }
    }

    @Test
    public void testImmediateNullValue() {
        try {
            Object value = Future.of(null).get();
            assertNull(value);
        }
        catch (Exception e) {
            fail("get() must not fail");
        }
    }

    @Test
    public void testConnectCallbackSuccess() {
        Future<String> future = proxy.call("longReply", "plaf");

        // the callback may be called from another thread
        final AtomicBoolean finished = new AtomicBoolean(false);
        /**
         * future.connect(new Future.Callback<String>() {
         *
         * @Override public void onFinished(Future<String> future) {
         *           finished.set(true); } });
         **/

        Future<Void> futureFinished = future.map(new Function<Future<String>, Void>() {
            @Override
            public Void execute(Future<String> future) throws Throwable {
                finished.set(true);
                return null;
            }
        });
        try {
            futureFinished.get();
        }
        catch (Exception e) {
            fail("get() must not fail");
        }

        assertTrue(finished.get());
    }

    @Test
    public void testConnectCallbackOnFailure() {
        Future<Void> future = proxy.call("throwUp");

        // the callback may be called from another thread
        final AtomicBoolean finished = new AtomicBoolean();
        Future<Void> futureFinished = future.map(new Function<Future<Void>, Void>() {
            @Override
            public Void execute(Future<Void> future) throws Throwable {
                assertTrue(future.hasError());
                finished.set(true);
                // We getting the result of the future to propagate the
                // exception.
                future.get();
                return null;
            }
        });

        try {
            futureFinished.get();
            fail("get() must fail");
        }
        catch (Exception e) {
            // expected exception
        }
        assertTrue(futureFinished.hasError());
        assertTrue(finished.get());
    }

    @Test
    public void testAndThenCallbackFailure() {
        Future<Void> future = proxy.call("throwUp");

        // the callback may be called from another thread
        final AtomicBoolean finished = new AtomicBoolean();
        Future<Void> futureFinished = future.andMap(new Function<Void, Void>() {
            @Override
            public Void execute(Void value) throws Throwable {
                assertTrue(false);
                finished.set(true);
                return null;
            }
        });

        try {
            futureFinished.get();
            fail("get() must fail");
        }
        catch (Exception e) {
            // expected exception
        }
        assertTrue(futureFinished.hasError());
        assertFalse(finished.get());
    }

    @Test
    public void testLongCall() {
        Future<String> fut = null;

        // Call a 2s long function
        try {
            fut = proxy.call("longReply", "plaf");
        }
        catch (DynamicCallException e) {
            fail("Error calling answer function : " + e.getMessage());
        }

        // Wait for call to finish.
        int count = 0;
        while (fut.isDone() == false) {
            count++;
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
            }
        }

        assertTrue("isDone() must return false at least 3 times (" + count + ")", count > 3);

        // Get and print result
        try {
            String result = fut.get();
            assertEquals("plafbim !", result);
        }
        catch (Exception e) {
            fail("Call has been interrupted (" + e.getMessage() + ")");
        }
    }

    @Test
    public void testGetTimeout() {
        System.out.println("testGetTimeout...");
        Future<String> fut = null;

        // Call a 2s long function
        try {
            fut = proxy.call("longReply", "plaf");
        }
        catch (DynamicCallException e) {
            System.out.println("Error calling answer function : " + e.getMessage());
            return;
        }

        boolean hasTimeout = false;
        try {
            fut.get(200, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException e) {
            hasTimeout = true;
        }
        catch (Exception e) {
        }

        assertTrue("Future.get() must timeout", hasTimeout);

        try {
            String ret = fut.get();
            assertEquals("plafbim !", ret);
        }
        catch (Exception e1) {
            fail("InterruptedException must not be thrown");
        }
    }

    @Test
    public void testGetTimeoutSuccess() {
        System.out.println("testGetTimeoutSuccess...");
        Future<String> fut = null;

        // Call a 2s long function
        try {
            fut = proxy.call("longReply", "plaf");
        }
        catch (DynamicCallException e) {
            System.out.println("Error calling answer function : " + e.getMessage());
            return;
        }

        String ret = null;
        try {
            ret = fut.get(3, TimeUnit.SECONDS);
            System.out.println("Got ret");
        }
        catch (TimeoutException e) {
            fail("Call must not timeout");
        }
        catch (Exception e1) {
            fail("InterruptedException must not be thrown");
        }

        assertEquals("plafbim !", ret);
    }

    @Test
    public void testTimeout() throws ExecutionException {
        System.out.println("testTimeout...");
        Future<Void> fut = null;
        try {
            fut = proxy.call("longReply", "plaf");
            fut.sync(150, TimeUnit.MILLISECONDS);
        }
        catch (Exception e) {
        }

        assertFalse(fut.isDone());
        fut.sync(150, TimeUnit.MILLISECONDS);
        assertFalse(fut.isDone());
        fut.sync(500, TimeUnit.SECONDS);
        assertTrue(fut.isDone());
        assertEquals("plafbim !", fut.get());
    }

    @Test
    public void testSessionTimeout() {
        Session test = new Session();
        Future<Void> fut = null;

        try {
            // Arbitrary chosen non valid address
            fut = test.connect("tcp://198.18.0.1:9559");
            fut.sync(1, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            fail("No way! : " + e.getMessage());
        }

        assertFalse(fut.isDone());
        assertFalse(test.isConnected());
    }

    public void testExecutionException() {
        try {
            proxy.call("throwUp").get();
        }
        catch (ExecutionException e) {
            assertTrue("ExecutionException cause should extend QiException", e.getCause() instanceof QiException);
            return;
        }
        fail("Must have thrown ExecutionException");
    }

    private class CancellableOperation {
        final AtomicBoolean onCancelCalled = new AtomicBoolean();

        public void doWork(Promise<String> promise) {
            for (int i = 0; i < 100; i++) {
                System.out.println("Printing " + i);
                if (onCancelCalled.get() == true)
                    return;
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            promise.setValue("OK");
        }

        public Future<String> longReply() {
            final Promise<String> promise = new Promise<String>();
            promise.setOnCancel(new Promise.CancelRequestCallback<String>() {
                @Override
                public void onCancelRequested(Promise<String> promise) {
                    onCancelCalled.set(true);
                    promise.setCancelled();
                }
            });
            new Thread(new Runnable() {
                @Override
                public void run() {
                    doWork(promise);
                }
            }).start();
            return promise.getFuture();
        }
    }

    @Test(expected = CancellationException.class)
    public void testCancelMatchesJavaFutureSemantics() throws ExecutionException {
        CancellableOperation cancellable = new CancellableOperation();
        Future<String> future = cancellable.longReply();
        future.cancel(true);
        future.get();
    }

    @Test(expected = CancellationException.class)
    public void testThenBackwardCancel() throws ExecutionException {
        CancellableOperation cancellable = new CancellableOperation();
        Future<String> future = cancellable.longReply();
        Future<Void> childFuture = future.map(new Function<Future<String>, Void>() {
            @Override
            public Void execute(Future<String> future) throws Throwable {
                return null;
            }
        });
        childFuture.cancel(true);
        future.get();
    }

    // @Test(expected = CancellationException.class)
    public void testThenForwardCancel() throws ExecutionException {
        final Future<String> future = Future.of("Test");
        final Future<String> otherFuture = proxy.call(String.class, "getCancellableFuture", "toto");
        final Future<String> childFuture = future.map(new Function<Future<String>, String>() {
            @Override
            public String execute(Future<String> future) throws Throwable {
                return otherFuture.get();
            }
        });

        childFuture.cancel(true);
        otherFuture.get();
    }

    private static class AsyncWait implements Runnable {
        enum Type {
            VALUE, ERROR, CANCEL
        }

        private final Promise<Long> promise;
        private final long milliseconds;
        private final Type finishType;

        AsyncWait(Promise<Long> promise, long milliseconds, Type finishType) {
            this.promise = promise;
            this.milliseconds = milliseconds;
            this.finishType = finishType;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(milliseconds);
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            switch (finishType) {
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

    @Test(expected = ExecutionException.class)
    public void testWaitAllWithError() throws ExecutionException, TimeoutException {
        Promise<Long> p1 = new Promise<Long>();
        Promise<Long> p2 = new Promise<Long>();
        new Thread(new AsyncWait(p1, 100, AsyncWait.Type.ERROR)).start();
        new Thread(new AsyncWait(p2, 200, AsyncWait.Type.VALUE)).start();
        Future<Long> f1 = p1.getFuture();
        Future<Long> f2 = p2.getFuture();
        Future.waitAll(f1, f2).get();
    }

    @Test(expected = CancellationException.class)
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

    @Test
    public void testFutureWaitFor() throws ExecutionException, TimeoutException {
        Promise<Long> p = new Promise<Long>();
        Future<Integer> future = Future.of(42).waitFor(p.getFuture());
        new Thread(new AsyncWait(p, 50, AsyncWait.Type.VALUE)).start();
        assertFalse(future.isDone());
        future.sync();
        assertTrue(p.getFuture().isDone());
        p.getFuture().get(0, TimeUnit.SECONDS); // must not throw
    }

    // @Test
    public void testAdvertisedFutureReturn() throws ExecutionException, InterruptedException {
        Future f = proxy.call(Future.class, "getFuture", "toto");
        assertEquals("ENDtoto", f.get());
    }

    // @Test
    public void testFutureCancelAdvertisedMethod() throws ExecutionException {
        Future f = proxy.call(Future.class, "getCancellableFuture", "toto");
        f.cancel(true);
        assertTrue(f.isCancelled());
    }

    // @Test(expected = CancellationException.class)
    public void testCancelPropagationOnWaitAll() throws ExecutionException, TimeoutException, InterruptedException {
        Future cancellableFut = proxy.call(Future.class, "getCancellableFuture", "toto");
        Future otherCancellableFut = proxy.call(Future.class, "getCancellableFuture", "toto");
        Future f = Future.waitAll(cancellableFut, otherCancellableFut);
        assertTrue(f.cancel(true));
        otherCancellableFut.get();
    }

    /**
     * Test of "then" in optimal condition (All succeed)
     */
    @Test
    public void testThenSucceed() {
        SleepThread sleepThread = new SleepThread(10, 42);
        Future<Integer> first = sleepThread.future();
        SleepFunction2 sleepFunction2 = new SleepFunction2(10, 73);
        Future<Integer> result = first.map(sleepFunction2);

        try {
            Assert.assertEquals(73, (int) result.get());
            Assert.assertEquals(42, (int) first.get());
            Assert.assertEquals(SleepThread.Status.SUCCEED, sleepThread.status());
            Assert.assertFalse(sleepThread.cancelRequested());
            Assert.assertTrue(sleepFunction2.executed());
        }
        catch (Exception exception) {
            Assert.fail("Unexpected error: " + exception);
            exception.printStackTrace();
        }
    }

    /**
     * Future result = future.then(continuation)<br>
     * Test if "future" cancelled before it finished
     */
    @Test
    public void testThenFirstNotFinishedFirstCancelled() {
        SleepThread sleepThread = new SleepThread(1000, 42);
        Future<Integer> first = sleepThread.future();
        SleepFunction2 sleepFunction2 = new SleepFunction2(10, 73);
        Future<Integer> result = first.map(sleepFunction2);
        first.requestCancellation();

        try {
            result.sync();
            Assert.assertFalse(result.isCancelled());
            Assert.assertTrue(first.isCancelled());
            Assert.assertEquals(SleepThread.Status.CANCELLED, sleepThread.status());
            Assert.assertTrue(sleepThread.cancelRequested());
            Assert.assertTrue(sleepFunction2.executed());
        }
        catch (Exception exception) {
            Assert.fail("Unexpected error: " + exception);
            exception.printStackTrace();
        }
    }

    /**
     * Future result = future.then(continuation)<br>
     * Test if "future" cancelled after it finished
     */
    @Test
    public void testThenFisrtFinishedFirstCancelled() {
        SleepThread sleepThread = new SleepThread(1, 42);
        Future<Integer> first = sleepThread.future();
        SleepFunction2 sleepFunction2 = new SleepFunction2(1000, 73);
        Future<Integer> result = first.map(sleepFunction2);

        try {
            Thread.sleep(250);
        }
        catch (Exception ignored) {
        }

        first.requestCancellation();

        try {
            result.sync();
            Assert.assertFalse(result.isCancelled());
            Assert.assertFalse(first.isCancelled());
            Assert.assertEquals(SleepThread.Status.SUCCEED, sleepThread.status());
            Assert.assertFalse(sleepThread.cancelRequested());
            Assert.assertTrue(sleepFunction2.executed());
        }
        catch (Exception exception) {
            Assert.fail("Unexpected error: " + exception);
            exception.printStackTrace();
        }
    }

    /**
     * Future result = future.then(continuation)<br>
     * Test if "result" cancelled before "future" finished
     */
    @Test
    public void testThenFirstNotFinishedSecondCancelled() {
        SleepThread sleepThread = new SleepThread(1000, 42);
        Future<Integer> first = sleepThread.future();
        SleepFunction2 sleepFunction2 = new SleepFunction2(10, 73);
        Future<Integer> result = first.map(sleepFunction2);
        result.requestCancellation();

        try {
            result.sync();
            Assert.assertFalse(result.isCancelled());
            Assert.assertTrue(first.isCancelled());
            Assert.assertEquals(SleepThread.Status.CANCELLED, sleepThread.status());
            Assert.assertTrue(sleepThread.cancelRequested());
            Assert.assertTrue(sleepFunction2.executed());
        }
        catch (Exception exception) {
            Assert.fail("Unexpected error: " + exception);
            exception.printStackTrace();
        }
    }

    /**
     * Future result = future.then(continuation)<br>
     * Test if "result" cancelled after "future" finished
     */
    @Test
    public void testThenFirstFinishedSecondCancelled() {
        SleepThread sleepThread = new SleepThread(1, 42);
        Future<Integer> first = sleepThread.future();
        SleepFunction2 sleepFunction2 = new SleepFunction2(1000, 73);
        Future<Integer> result = first.map(sleepFunction2);

        try {
            Thread.sleep(250);
        }
        catch (Exception ignored) {
        }

        result.requestCancellation();

        try {
            result.sync();
            Assert.assertFalse(result.isCancelled());
            Assert.assertFalse(first.isCancelled());
            Assert.assertEquals(SleepThread.Status.SUCCEED, sleepThread.status());
            Assert.assertFalse(sleepThread.cancelRequested());
            Assert.assertTrue(sleepFunction2.executed());
        }
        catch (Exception exception) {
            Assert.fail("Unexpected error: " + exception);
            exception.printStackTrace();
        }
    }

    /**
     * Test of "andThen" in optimal condition (All succeed)
     */
    @Test
    public void testAndThenSucceed() {
        SleepThread sleepThread = new SleepThread(10, 42);
        Future<Integer> first = sleepThread.future();
        SleepFunction sleepFunction = new SleepFunction(10, 73);
        Future<Integer> result = first.andMap(sleepFunction);

        try {
            Assert.assertEquals(73, (int) result.get());
            Assert.assertEquals(42, (int) first.get());
            Assert.assertEquals(SleepThread.Status.SUCCEED, sleepThread.status());
            Assert.assertFalse(sleepThread.cancelRequested());
            Assert.assertTrue(sleepFunction.executed());
        }
        catch (Exception exception) {
            Assert.fail("Unexpected error: " + exception);
            exception.printStackTrace();
        }
    }

    /**
     * Future result = future.andThen(continuation)<br>
     * Test if "future" cancelled before it finished
     */
    @Test
    public void testAndThenFirstNotFinishedFirstCancelled() {
        SleepThread sleepThread = new SleepThread(1000, 42);
        Future<Integer> first = sleepThread.future();
        SleepFunction sleepFunction = new SleepFunction(10, 73);
        Future<Integer> result = first.andMap(sleepFunction);
        first.requestCancellation();

        try {
            result.sync();
            Assert.assertTrue(result.isCancelled());
            Assert.assertTrue(first.isCancelled());
            Assert.assertEquals(SleepThread.Status.CANCELLED, sleepThread.status());
            Assert.assertTrue(sleepThread.cancelRequested());
            Assert.assertFalse(sleepFunction.executed());
        }
        catch (Exception exception) {
            Assert.fail("Unexpected error: " + exception);
            exception.printStackTrace();
        }
    }

    /**
     * Future result = future.andThen(continuation)<br>
     * Test if "future" cancelled after it finished
     */
    @Test
    public void testAndThenFisrtFinishedFirstCancelled() {
        SleepThread sleepThread = new SleepThread(1, 42);
        Future<Integer> first = sleepThread.future();
        SleepFunction sleepFunction = new SleepFunction(1000, 73);
        Future<Integer> result = first.andMap(sleepFunction);

        try {
            Thread.sleep(250);
        }
        catch (Exception ignored) {
        }

        first.requestCancellation();

        try {
            result.sync();
            Assert.assertFalse(result.isCancelled());
            Assert.assertFalse(first.isCancelled());
            Assert.assertEquals(SleepThread.Status.SUCCEED, sleepThread.status());
            Assert.assertFalse(sleepThread.cancelRequested());
            Assert.assertTrue(sleepFunction.executed());
        }
        catch (Exception exception) {
            Assert.fail("Unexpected error: " + exception);
            exception.printStackTrace();
        }
    }

    /**
     * Future result = future.andThen(continuation)<br>
     * Test if "result" cancelled before "future" finished
     */
    @Test
    public void testAndThenFirstNotFinishedSecondCancelled() {
        SleepThread sleepThread = new SleepThread(1000, 42);
        Future<Integer> first = sleepThread.future();
        SleepFunction sleepFunction = new SleepFunction(10, 73);
        Future<Integer> result = first.andMap(sleepFunction);
        result.requestCancellation();

        try {
            result.sync();
            Assert.assertTrue(result.isCancelled());
            Assert.assertTrue(first.isCancelled());
            Assert.assertEquals(SleepThread.Status.CANCELLED, sleepThread.status());
            Assert.assertTrue(sleepThread.cancelRequested());
            Assert.assertFalse(sleepFunction.executed());
        }
        catch (Exception exception) {
            Assert.fail("Unexpected error: " + exception);
            exception.printStackTrace();
        }
    }

    /**
     * Future result = future.andThen(continuation)<br>
     * Test if "result" cancelled after "future" finished
     */
    @Test
    public void testAndThenFirstFinishedSecondCancelled() {
        SleepThread sleepThread = new SleepThread(1, 42);
        Future<Integer> first = sleepThread.future();
        SleepFunction sleepFunction = new SleepFunction(1000, 73);
        Future<Integer> result = first.andMap(sleepFunction);

        try {
            Thread.sleep(250);
        }
        catch (Exception ignored) {
        }

        result.requestCancellation();

        try {
            result.sync();
            Assert.assertFalse(result.isCancelled());
            Assert.assertFalse(first.isCancelled());
            Assert.assertEquals(SleepThread.Status.SUCCEED, sleepThread.status());
            Assert.assertFalse(sleepThread.cancelRequested());
            Assert.assertTrue(sleepFunction.executed());
        }
        catch (Exception exception) {
            Assert.fail("Unexpected error: " + exception);
            exception.printStackTrace();
        }
    }
}
