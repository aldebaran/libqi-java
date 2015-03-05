/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

public class QiException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Exception thrown when error occurs during a QiMessaging operation.
   * @param e Error message.
   */
  public QiException(String e)
  {
    super(e);
  }

  /**
   * Exception thrown when error occurs during a QiMessaging operation.
   */
  public QiException()
  {
    super();
  }

}
