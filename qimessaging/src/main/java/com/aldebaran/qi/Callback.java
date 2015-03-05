/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

public interface Callback<T>
{

  /**
   * Called when future completes successfully.
   * @param future Successful Future
   * @param args Arguments given to Future.addCallback() method.
   */
  public void onSuccess(Future<T> future, Object[] args);

  /**
   * Called when future ends with error.
   * @param future Future with error
   * @param args Arguments given to Future.addCallback() method.
   */
  public void onFailure(Future<T> future, Object[] args);

  /**
   * Called when future completes.
   * @param future Completed Future
   * @param args Arguments given to Future.addCallback() method.
   */
  public void onComplete(Future<T> future, Object[] args);

}
