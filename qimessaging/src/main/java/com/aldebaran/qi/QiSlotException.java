package com.aldebaran.qi;

public class QiSlotException extends QiRuntimeException
{

  public QiSlotException() {
    // empty
  }

  public QiSlotException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public QiSlotException(String message)
  {
    super(message);
  }

  public QiSlotException(Throwable cause)
  {
    super(cause);
  }
}
