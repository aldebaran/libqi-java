/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#ifndef _JAVA_JNI_MAP_HPP_
#define _JAVA_JNI_MAP_HPP_

#include <jni.h>

/**
 * @brief The JNIMap class Helper class to manipulation Java Map<?, ?> in C++
 */
class JNIMap
{
  public:
    JNIMap();
    JNIMap(jobject obj);

    int     size();
    jobjectArray keys();
    jobject get(jobject key);
    jobject object();
    void    put(jobject key, jobject value);

  private:
    jobject _obj;
    JNIEnv* _env;

};

#endif // !_JAVA_JNI_MAP_HPP_
