/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran;

import com.aldebaran.qi.CallError;
import com.aldebaran.qi.AnyObject;

public class EventTester {

  public void onFire(Integer i)
  {
    System.out.println("onFire !");
  }

  public void testEvent(AnyObject proxy)
  {
    try {
      proxy.connect("fire", "onFire", this);
    } catch (Exception e1) {
      System.out.println("Cannot connect onFire callback to fire event");
    }
    try {
      proxy.call("triggerFireEvent", 42);
    } catch (CallError e) {
      System.out.println("Error triggering Fire event : " + e.getMessage());
    }
  }

}
