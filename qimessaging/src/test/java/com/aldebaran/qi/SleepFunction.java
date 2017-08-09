package com.aldebaran.qi;

public class SleepFunction implements Function<Integer, Integer> {
    private final SleepThread sleepThread;
    private boolean executed;

    public SleepFunction(final int sleepTime, final int value) {
        this.sleepThread = new SleepThread(sleepTime, value);
        this.executed = false;
    }

    @Override
    public Integer execute(Integer value) throws Throwable {
        this.executed = true;
        System.out.println("SleepFunction exectuted: " + value);
        return this.sleepThread.future().get();
    }

    public boolean executed() {
        return this.executed;
    }
}
