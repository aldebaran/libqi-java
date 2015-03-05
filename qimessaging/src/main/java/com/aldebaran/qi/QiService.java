/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

/**
 * @since 1.20
 * Implement this interface to create a Qimessaging service.
 */
public abstract class QiService {

  protected AnyObject self;

  public void init(AnyObject self)
  {
    this.self = self;
  }

}
