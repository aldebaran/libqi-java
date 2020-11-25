/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2013 Aldebaran Robotics
** See COPYING for the license
*/

#ifndef _JAVA_JNI_TUPLE_HPP_
#define _JAVA_JNI_TUPLE_HPP_

#include <jni.h>
#include "jnitools.hpp"

/**
 * @brief The JNITuple class Helper class to manipulate com.aldebaran.qimessaging.Tuple classes.
 */
class JNITuple
{
  public:
    JNITuple(jobject obj);

    int size() const;
    qi::jni::ScopedJObject<jobject> get(int index) const;

    void set(int index, jobject obj);
    jobject object() const;

  private:

    jobject _obj;
    JNIEnv* _env;
};

#endif // !_JAVA_JNI_TUPLE_HPP_
