package com.aldebaran.qi;

public class SleepFutureFunction implements Function<Integer, Future<Integer>> {
    private final SleepThread sleepThread;
    private boolean executed;

    public SleepFutureFunction(final int sleepTime, final int value) {
        this.sleepThread = new SleepThread(sleepTime, value);
        this.executed = false;
    }

    @Override
    public Future<Integer> execute(Integer value) throws Throwable {
        this.executed = true;
        System.out.println("SleepFutureFunction exectuted: " + value);
        return this.sleepThread.future();
    }

    public boolean executed() {
        return this.executed;
    }
}
