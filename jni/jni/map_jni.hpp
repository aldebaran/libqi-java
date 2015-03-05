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
#include <enumeration_jni.hpp>

/**
 * @brief The JNIHashTable class Helper class to manipulation Java Hashtable<?, ?> (map) in C++
 */
class JNIHashTable
{
  public:
    JNIHashTable();
    JNIHashTable(jobject obj);
    ~JNIHashTable();

    int     size();
    JNIEnumeration keys();
    jobject at(jobject key);
    jobject object();
    bool    setItem(jobject key, jobject value);

    jobject at(int pos);
    bool    next(int *pos, jobject* key, jobject* value);

  private:
    jobject _obj;
    jclass  _cls;
    JNIEnv* _env;

};

#endif // !_JAVA_JNI_MAP_HPP_
