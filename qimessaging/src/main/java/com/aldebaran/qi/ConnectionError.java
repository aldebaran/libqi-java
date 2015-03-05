/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

@SuppressWarnings("serial")
public final class ConnectionError extends Exception
{

  public ConnectionError(String e)
  {
    super(e);
  }

  public ConnectionError()
  {
  }

}
