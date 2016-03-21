package com.aldebaran.qi;

@SuppressWarnings("serial")
public class QiRuntimeException extends RuntimeException
{

  public QiRuntimeException()
  {
  }

  public QiRuntimeException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public QiRuntimeException(String message)
  {
    super(message);
  }

  public QiRuntimeException(Throwable cause)
  {
    super(cause);
  }
}
