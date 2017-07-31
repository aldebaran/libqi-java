package com.aldebaran.qi;

import java.util.concurrent.CancellationException;

public class SleepConsumer2 implements Consumer<Future<Integer>> {
    private boolean consumed;

    public SleepConsumer2() {
        this.consumed = false;
    }

    @Override
    public void consume(Future<Integer> value) throws Throwable {
        this.consumed = true;

        switch (value.get()) {
            case SleepThread.ERROR_VALUE:
                System.out.println("SleepConsumer errored");
                throw new Exception("SleepConsumer evil!");
            case SleepThread.CANCEL_VALUE:
                System.out.println("SleepConsumer cancelled");
                throw new CancellationException("SleepConsumer cancelled!");
        }

        System.out.println("SleepFunction consumed: " + value);
    }

    public boolean consumed() {
        return this.consumed;
    }
}
