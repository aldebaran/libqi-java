/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

@SuppressWarnings("serial")
public class CallError extends Exception
{

  /**
   * Exception thrown when error occur during a qimessaging call.
   * @param e Error message.
   */
  public CallError(String e)
  {
    super(e);
  }

  /**
   * Exception thrown when error occurs during a call.
   * Exception thrown when error occur during a qimessaging call.
   */
  public CallError()
  {
    super();
  }

}
