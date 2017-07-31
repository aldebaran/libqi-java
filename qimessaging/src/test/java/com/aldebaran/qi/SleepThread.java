package com.aldebaran.qi;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aldebaran.qi.Promise.CancelRequestCallback;

public class SleepThread extends Thread implements CancelRequestCallback<Integer> {
    public static final int ERROR_VALUE = 666;
    public static final int CANCEL_VALUE = -999;

    public static enum Status {
        RUNNING, SUCCEED, FAILED, CANCELLED
    }

    private final AtomicBoolean waiting;
    private final int sleepTime;
    private Status status;
    private final int value;
    private final Promise<Integer> promise;
    private final AtomicBoolean started;
    private boolean cancelRequested;

    public SleepThread(final int sleepTime, final int value) {
        this.status = Status.RUNNING;
        this.waiting = new AtomicBoolean(false);
        this.sleepTime = Math.max(1, sleepTime);
        this.value = value;
        this.promise = new Promise<Integer>();
        this.started = new AtomicBoolean(false);
        this.promise.setOnCancel(this);
        this.cancelRequested = false;
    }

    public Future<Integer> future() {
        synchronized (this.started) {
            if (!this.started.get()) {
                this.started.set(true);
                this.start();
            }
        }

        return this.promise.getFuture();
    }

    public Status status() {
        synchronized (this.started) {
            return this.status;
        }
    }

    @Override
    public void run() {
        if (this.value == SleepThread.ERROR_VALUE) {
            System.out.println("born #FAILED#");
            this.status = Status.FAILED;
            this.promise.setError("Number of the beast!");
            return;
        }

        if (this.value == SleepThread.CANCEL_VALUE) {
            System.out.println("born #CANCELLED#");
            this.status = Status.CANCELLED;
            this.promise.setCancelled();
            return;
        }

        synchronized (this.started) {
            this.waiting.set(true);

            try {
                this.started.wait(this.sleepTime);
            }
            catch (Exception ignored) {
            }

            this.waiting.set(false);
        }

        switch (this.status) {
            case RUNNING:
            case SUCCEED:
                System.out.println("#SUCCEED#" + this.value);
                this.status = Status.SUCCEED;
                this.promise.setValue(this.value);
                break;
            case CANCELLED:
                System.out.println("#CANCELLED#");
                this.promise.setCancelled();
                break;
            case FAILED:
                System.out.println("#FAILED#");
                this.promise.setError("Shouldn't goes here!");
                break;
        }
    }

    @Override
    public void onCancelRequested(final Promise<Integer> promise) {
        System.out.println("--- onCancelRequested ---");
        this.cancelRequested = true;

        synchronized (this.started) {
            if (this.status == Status.RUNNING) {
                this.status = Status.CANCELLED;
            }

            if (this.waiting.get()) {
                this.started.notify();
            }
        }
    }

    public boolean cancelRequested() {
        return this.cancelRequested;
    }
}
