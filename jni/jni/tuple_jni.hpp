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

/**
 * @brief The JNITuple class Helper class to manipulate com.aldebaran.qimessaging.Tuple classes.
 */
class JNITuple
{
  public:
    JNITuple(jobject obj);

    int size();
    jobject get(int index);
    void set(int index, jobject obj);
    jobject object();

  private:

    jobject _obj;
    JNIEnv* _env;
};

#endif // !_JAVA_JNI_TUPLE_HPP_
