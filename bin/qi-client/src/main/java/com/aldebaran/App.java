/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.aldebaran.qi.CallError;
import com.aldebaran.qi.AnyObject;
import com.aldebaran.qi.Session;
import com.aldebaran.qi.Application;

public class App
{

  public static void testFloat(AnyObject proxy)
  {
    Float answer = null;

    try
    {
      answer = proxy.<Float>call("answerFloat", 41.2).get();
    } catch (CallError e)
    {
      System.out.println("Error calling answerFloat() :" + e.getMessage());
      return;
    } catch (Exception e)
    {
      System.out.println("Unexpected error calling answerFloat() : " + e.getMessage());
      return;
    }

    System.out.println("AnswerFloat : " + answer);
  }

  public static void testInteger(AnyObject proxy)
  {
    Integer answer = null;

    try
    {
      answer = proxy.<Integer>call("answer", 41).get();
    } catch (Exception e)
    {
      System.out.println("Error calling answer() :" + e.getMessage());
      return;
    }

    if (answer == null)
      System.out.println("Answer is null :(");
    else
      System.out.println("AnswerInteger : " + answer);
  }

  public static void testBoolean(AnyObject proxy)
  {
    Boolean answer = null;

    try
    {
      answer = proxy.<Boolean>call("answerBool", false).get();
    } catch (Exception e)
    {
      System.out.println("Error calling answerBool() :" + e.getMessage());
      return;
    }

    System.out.println("AnswerBool : " + answer);
  }

  public static void testAdd(AnyObject proxy)
  {
    Integer answer = null;

    try
    {
      answer = proxy.<Integer>call("add", 40, 2).get();
    } catch (Exception e)
    {
      System.out.println("Error calling add() :" + e.getMessage());
      return;
    }

    System.out.println("add : " + answer);
  }

  public static void testMap(AnyObject proxy)
  {
    Map<Integer, Boolean> abacus = new Hashtable<Integer, Boolean>();
    Map<Integer, Boolean> answer = null;

    abacus.put(1, false);
    abacus.put(2, true);
    abacus.put(4, true);

    try
    {
      answer = proxy.<Hashtable<Integer, Boolean> >call("abacus", abacus).get();
    }
    catch (Exception e)
    {
      System.out.println("Error calling abacus() :" + e.getMessage());
      return;
    }

    System.out.println("abacus : " + answer);
  }

  public static void testList(AnyObject proxy)
  {
    ArrayList<Integer> positions = new ArrayList<Integer>();
    ArrayList<Integer> answer = null;

    positions.add(40);
    positions.add(3);
    positions.add(3);
    positions.add(2);

    try
    {
      answer = proxy.<ArrayList<Integer> >call("echoIntegerList", positions).get();
    } catch (Exception e)
    {
      System.out.println("Error calling echoIntegerList() :" + e.getMessage());
      return;
    }

    System.out.println("list : " + answer);
  }

  public static void testString(AnyObject proxy)
  {
    String  str = null;

    try
    {
      str = proxy.<String>call("reply", "plaf").get();
    } catch (Exception e)
    {
      System.out.println("Error calling reply() :" + e.getMessage());
      return;
    }

    System.out.println("AnswerString : " + str);
  }

  public static void testObject(AnyObject proxy) throws InterruptedException, ExecutionException
  {
    AnyObject ro = null;
    try
    {
      ro = proxy.<AnyObject>call("createObject").get();
    } catch (Exception e)
    {
      System.out.println("Call failed: " + e.getMessage());
      return;
    }

    String prop = (String) ro.<String>property("name").get();
    System.out.println("Property : " + prop);
  }

  public static void main( String args[] ) throws InterruptedException, ExecutionException
  {
    @SuppressWarnings("unused")
    Application app = new Application(args);
    Session client = new Session();
    String  sdAddr = "tcp://127.0.0.1:9559";


    if (args.length >= 1)
      sdAddr = args[0];

    try {
      client.connect(sdAddr);
    } catch (Exception e)
    {
      System.out.println("Cannot connect to ServiceDirectory : "+e.getMessage());
      return;
    }

    AnyObject proxy = null;
    try {
      proxy = client.service("serviceTest");
    } catch (Exception e) {
      System.out.println("Cannot get proxy on serviceTest");
      return;
    }


    testObject(proxy);
    testMap(proxy);
    testList(proxy);
    testString(proxy);

    testInteger(proxy);
    testFloat(proxy);
    testBoolean(proxy);
    testAdd(proxy);

    EventTester t = new EventTester();
    t.testEvent(proxy);

    System.exit(0);
  }
}
