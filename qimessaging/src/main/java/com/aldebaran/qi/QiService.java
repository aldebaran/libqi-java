/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

/**
 * Implement this interface to create a Qimessaging service.
 */
public abstract class QiService {

  protected AnyObject self;

  public void init(AnyObject self)
  {
    this.self = self;
  }

}
