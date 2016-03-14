/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

public class AdvertisementException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Exception thrown when error occurs during a QiMessaging operation.
   * @param e Error message.
   */
  public AdvertisementException(String e)
  {
    super(e);
  }

  /**
   * Exception thrown when error occurs during a QiMessaging operation.
   */
  public AdvertisementException()
  {
    super();
  }

}
