package com.aldebaran.qi;

@SuppressWarnings("serial")
public class QiException extends Exception
{

  public QiException()
  {
  }

  public QiException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public QiException(String message)
  {
    super(message);
  }

  public QiException(Throwable cause)
  {
    super(cause);
  }
}
