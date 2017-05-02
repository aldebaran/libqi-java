/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

/**
 * Interface used to create a service that can be registered to a {@link Session}
 */
public abstract class QiService {

    protected AnyObject self;

    public void init(AnyObject self) {
        this.self = self;
    }

}
