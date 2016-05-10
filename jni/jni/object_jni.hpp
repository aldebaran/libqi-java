/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#ifndef _JAVA_JNI_OBJECT_JNI_HPP_
#define _JAVA_JNI_OBJECT_JNI_HPP_

#include <jni.h>

#include <qi/anyobject.hpp>

/**
 * @brief The JNIObject class Helper for conversion between qi::AnyObject and com.aldebaran.qimessaging.Object
 */
class JNIObject
{
  public:
    JNIObject(const qi::AnyObject& o);
    JNIObject(qi::AnyObject* o);
    JNIObject(jobject value);

    jobject object();
    qi::AnyObject      objectPtr();

  private:
    void build(qi::AnyObject *o);

    qi::jni::JNIAttach attach;

    jobject _obj;
    JNIEnv* _env;
};

#endif // !_JAVA_JNI_OBJECT_JNI_HPP_
