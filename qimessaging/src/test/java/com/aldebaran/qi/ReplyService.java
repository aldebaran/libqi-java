/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReplyService extends QiService {
    private int storedValue = 0;
    private Promise<Void> cancellableTask = null;

    public Boolean iWillThrow() throws Exception {
        throw new Exception("Expected Failure");
    }

    public Boolean generic(Object obj) {
        if (obj == null)
            return false;

        System.out.println("AnyObject received : " + obj);
        return true;
    }

    public AnyObject createObject() {
        DynamicObjectBuilder ob = new DynamicObjectBuilder();

        try {
            ob.advertiseSignal("fire::(i)");

            ob.advertiseMethod("reply::s(s)", this, "Concatenate given parameter with 'bim !'");
            ob.advertiseMethod("answer::s()", this, "return '42 !'");
            ob.advertiseMethod("add::i(iii)", this, "Sum given parameters and return computed value");
            ob.advertiseMethod("throwUp::v()", this, "Throws");

            ob.advertiseProperty("name", String.class);
            ob.advertiseProperty("uid", Integer.class);
            ob.advertiseProperty("settings", HashMap.class);

        } catch (Exception e1) {
            System.out.println("Cannot advertise methods and signals : " + e1.getMessage());
            return null;
        }

        AnyObject ro = ob.object();

        try {
            ro.setProperty("name", "foo");
            ro.setProperty("uid", 42);
        } catch (Exception e) {
            System.out.println("Cannot set properties : " + e.getMessage());
        }

        return ro;
    }

    public AnyObject createNullObject() {
        return null;
    }

    public Tuple info(String str, Integer i, Boolean b) {
        Tuple ret = Tuple.of(str, i, b);
        System.out.println("Received : " + str + "," + i + "," + b);
        return ret;
    }

    public String reply(String s) {
        String reply = new String();

        reply = s;
        reply = reply.concat("bim !");
        System.out.printf("Replying : %s\n", reply);
        return reply;
    }

    public String longReply(String str) {
        System.out.println("Sleeping 500ms...");

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            System.out.println("Cannot sleep anymore : " + e.getMessage());
        }

        System.out.println("Replying : " + str + "bim !");
        return str.concat("bim !");
    }

    public String answer() {
        return "42 !";
    }

    public void printLong(Long l) {
        System.out.println("printLong : " + l);
    }

    public Integer answer(Integer val) {
        System.out.println("Replying : " + (val + 1));
        return val + 1;
    }

    public Float answerFloat(Float val) {
        return val + 1f;
    }

    public Boolean answerBool(Boolean val) {
        if (val == true)
            return false;

        return true;
    }

    public Integer add(Integer a, Integer b, Integer c) {
        System.out.println(a + " + " + b + " + " + c + " = " + (a + b + c));
        return a + b + c;
    }

    public Map<Integer, Boolean> abacus(Map<Integer, Boolean> map) {
        Map<Integer, Boolean> ret = new HashMap<Integer, Boolean>();

        System.out.println("abacus : Received args : " + map);
        try {
            for (Iterator<Integer> ii = map.keySet().iterator(); ii.hasNext(); ) {
                Integer key = (Integer) ii.next();
                Boolean value = map.get(key);
                Boolean newVal = false;

                if (value == false)
                    newVal = true;

                ret.put(key, newVal);
            }
        } catch (Exception e) {
            System.out.println("Exception caught :( " + e.getMessage());
        }

        System.out.println("Returning : " + ret);
        return ret;
    }

    public ArrayList<Float> echoFloatList(ArrayList<Float> l) {
        try {
            System.out.println("Received args : " + l);
            for (Iterator<Float> it = l.iterator(); it.hasNext(); ) {
                System.out.println("Value : " + it.next());
            }
        } catch (Exception e) {
            System.out.println("Exception caught :( " + e.getMessage());
        }
        return l;
    }

    public void setStored(Integer v) {
        storedValue = v;
    }

    public Integer waitAndAddToStored(Integer msDelay, Integer v) {
        try {
            Thread.sleep(msDelay);
        } catch (Exception e) {
        }
        return v + storedValue;
    }

    public void throwUp() {
        throw new RuntimeException("I has faild");
    }

    public Tuple genTuple() {
        return Tuple.of(42, "forty-two");
    }

    public ArrayList<Tuple> genTuples() {
        // services expect ArrayList, not List :(
        ArrayList<Tuple> tuples = new ArrayList<Tuple>();
        tuples.add(genTuple());
        return tuples;
    }

    public Integer getFirstFieldValue(Tuple tuple) {
        return (Integer) tuple.get(0);
    }

    private void doWork(Promise<String> promise, String res) {
        System.out.println("do Work");
        promise.setValue("END" + res);
    }

    private final AtomicBoolean onCancelCalled = new AtomicBoolean();

    private void doCancellableWork(Promise<String> promise, String res) {
        for (int i = 0; i < 3; i++) {
            System.out.println("do Cancellable Work " + i);
            if (onCancelCalled.get() == true)
                return;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        promise.setValue("END" + res);
    }

    public Future<String> getFuture(final String param) {
        final Promise<String> promise = new Promise<String>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                doWork(promise, param);
            }
        }).start();
        return promise.getFuture();
    }

    public Future<String> getCancellableFuture(final String param) {
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
                doCancellableWork(promise, param);
            }
        }).start();
        return promise.getFuture();
    }

    public Future<Void> getCancellableInfiniteFuture() {
        final Promise<Void> promise = new Promise<Void>();
        promise.setOnCancel(new Promise.CancelRequestCallback<Void>() {
            @Override
            public void onCancelRequested(Promise<Void> unusedPromise) {
                promise.setCancelled();
            }
        });
        System.out.println("getCancellableInfiniteFuture was called");
        return promise.getFuture();
    }
}
