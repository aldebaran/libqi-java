/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#ifndef _JAVA_JNI_SERVICEDIRECTORY_HPP_
#define _JAVA_JNI_SERVICEDIRECTORY_HPP_

#include <jni.h>

extern "C"
{
  JNIEXPORT jlong   Java_com_aldebaran_qi_ServiceDirectory_qiTestSDCreate(JNIEnv *env, jobject obj);
  JNIEXPORT void    Java_com_aldebaran_qi_ServiceDirectory_qiTestSDDestroy(JNIEnv *env, jobject obj, jlong pServiceDirectory);
  JNIEXPORT jstring Java_com_aldebaran_qi_ServiceDirectory_qiListenUrl(JNIEnv *env, jobject obj, jlong pServiceDirectory);
  JNIEXPORT void    Java_com_aldebaran_qi_ServiceDirectory_qiTestSDClose(JNIEnv *env, jobject obj, jlong pServiceDirectory);

} // !extern "C"

#endif // !_JAVA_JNI_SERVICEDIRECTORY_HPP_
