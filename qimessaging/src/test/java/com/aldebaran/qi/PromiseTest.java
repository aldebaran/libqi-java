package com.aldebaran.qi;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public class PromiseTest {
    @Test
    public void testPromiseValue() {
        Promise<Integer> promise = new Promise<Integer>();
        Future<Integer> future = promise.getFuture();
        promise.setValue(42);
        int value = future.getValue();
        Assert.assertEquals(42, value);
    }

    @Test
    public void testPromiseError() {
        Promise<Integer> promise = new Promise<Integer>();
        Future<Integer> future = promise.getFuture();
        promise.setError("something went wrong");
        String error = future.getErrorMessage();
        Assert.assertEquals("something went wrong", error);
    }

    @Test
    public void testPromiseCancelled() {
        Promise<Integer> promise = new Promise<Integer>();
        Future<Integer> future = promise.getFuture();
        promise.setCancelled();
        Assert.assertTrue(future.isCancelled());
    }

    @Test
    public void testBlockingGet() throws ExecutionException {
        final Promise<Integer> promise = new Promise<Integer>();
        Future<Integer> future = promise.getFuture();
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    promise.setValue(42);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }).start();
        try {
            future.get(50, TimeUnit.MILLISECONDS);
            Assert.fail("Value should not be available yet");
        } catch (TimeoutException e) {
            // expected exception
        }
        int value = future.get();
        Assert.assertEquals(42, value);
    }

    public static boolean isCallbackExecutedOnSameThread(FutureCallbackType futureCallbackType) throws InterruptedException {
        Promise<Void> promise = new Promise<Void>(futureCallbackType);
        promise.setValue(null);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicLong callbackThreadId = new AtomicLong();
        promise.getFuture().andThen(new Function<Void, Void>() {
            @Override
            public Void execute(Void value) throws Throwable {
                callbackThreadId.set(Thread.currentThread().getId());
                countDownLatch.countDown();
                return null;
            }
        });
        countDownLatch.await();
        return Thread.currentThread().getId() == callbackThreadId.get();
    }

    @Test
    public void testAsyncType() throws InterruptedException {
        Assert.assertFalse(isCallbackExecutedOnSameThread(FutureCallbackType.Async));
    }

    @Test
    public void testSyncType() throws InterruptedException {
        Assert.assertTrue(isCallbackExecutedOnSameThread(FutureCallbackType.Sync));
    }
}
