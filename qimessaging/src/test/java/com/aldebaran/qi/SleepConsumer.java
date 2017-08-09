package com.aldebaran.qi;

import java.util.concurrent.CancellationException;

public class SleepConsumer implements Consumer<Integer> {
    private boolean consumed;

    public SleepConsumer() {
        this.consumed = false;
    }

    @Override
    public void consume(Integer value) throws Throwable {
        this.consumed = true;

        switch (value) {
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
