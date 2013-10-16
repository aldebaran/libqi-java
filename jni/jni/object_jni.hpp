/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
*/

#ifndef _JAVA_JNI_OBJECT_JNI_HPP_
#define _JAVA_JNI_OBJECT_JNI_HPP_

#include <jni.h>

#include <qitype/anyobject.hpp>

/**
 * @brief The JNIObject class Helper for conversion between qi::AnyObject and com.aldebaran.qimessaging.Object
 */
class JNIObject
{
  public:
    JNIObject(const qi::AnyObject& o);
    JNIObject(qi::AnyObject* o);
    JNIObject(jobject value);
    ~JNIObject();

    jobject object();
    qi::AnyObject      objectPtr();

  private:
    void build(qi::AnyObject *o);

    jobject _obj;
    jclass  _cls;
    JNIEnv* _env;
};

#endif // !_JAVA_JNI_OBJECT_JNI_HPP_
