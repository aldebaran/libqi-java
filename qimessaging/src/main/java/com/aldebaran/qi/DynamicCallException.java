/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

@SuppressWarnings("serial")
public class DynamicCallException extends QiRuntimeException
{

  /**
   * Exception thrown when error occur during a qimessaging call.
   * @param e Error message.
   */
  public DynamicCallException(String e)
  {
    super(e);
  }

  /**
   * Exception thrown when error occurs during a call.
   * Exception thrown when error occur during a qimessaging call.
   */
  public DynamicCallException()
  {
    super();
  }

}
