/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

/**
 * An implementation of this interface can be connected to a {@link Future}
 * in order to be called when the {@link Future} becomes ready: when it
 * completes, it successfully completes or it ends with error.
 * <p>
 * The Callback will be called even if the {@link Future} is already ready. The
 * first argument is always the {@link Future} itself.
 *
 * @param <T> The type of the {@link Future}'s result
 */

public interface Callback<T> {

    /**
     * Called when future completes successfully.
     *
     * @param future Successful Future
     * @param args   Arguments given to Future.addCallback() method.
     */
    public void onSuccess(Future<T> future, Object[] args);

    /**
     * Called when future ends with error.
     *
     * @param future Future with error
     * @param args   Arguments given to Future.addCallback() method.
     */
    public void onFailure(Future<T> future, Object[] args);

    /**
     * Called when future completes.
     *
     * @param future Completed Future
     * @param args   Arguments given to Future.addCallback() method.
     */
    public void onComplete(Future<T> future, Object[] args);

}
