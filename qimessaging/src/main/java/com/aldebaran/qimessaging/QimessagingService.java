package com.aldebaran.qimessaging;

/**
 * @since 1.20
 * Implement this interface to create a Qimessaging service.
 */
public abstract class QimessagingService {

  protected AnyObject self;

  public void init(AnyObject self)
  {
    this.self = self;
  }

}
