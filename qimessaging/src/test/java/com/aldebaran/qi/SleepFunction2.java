package com.aldebaran.qi;

public class SleepFunction2 implements Function<Future<Integer>, Integer> {
    private final SleepThread sleepThread;
    private boolean executed;

    public SleepFunction2(final int sleepTime, final int value) {
        this.sleepThread = new SleepThread(sleepTime, value);
        this.executed = false;
    }

    @Override
    public Integer execute(Future<Integer> value) throws Throwable {
        this.executed = true;
        System.out.println("SleepFunction exectuted: " + value.get());
        return this.sleepThread.future().get();
    }

    public boolean executed() {
        return this.executed;
    }
}
