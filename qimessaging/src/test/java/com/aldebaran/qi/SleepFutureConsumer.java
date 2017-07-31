package com.aldebaran.qi;

import java.util.concurrent.CancellationException;

public class SleepFutureConsumer implements Consumer<Future<Integer>> {
    private boolean consumed;

    public SleepFutureConsumer() {
        this.consumed = false;
    }

    @Override
    public void consume(Future<Integer> future) throws Throwable {
        this.consumed = true;

        switch (future.get()) {
            case SleepThread.ERROR_VALUE:
                System.out.println("SleepFutureConsumer errored");
                throw new Exception("SleepFutureConsumer evil!");
            case SleepThread.CANCEL_VALUE:
                System.out.println("SleepFutureConsumer cancelled");
                throw new CancellationException("SleepFutureConsumer cancelled!");
        }

        System.out.println("SleepFutureConsumer consumed: " + future.get());
    }

    public boolean consumed() {
        return this.consumed;
    }
}
