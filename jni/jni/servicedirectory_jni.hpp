/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
*/

#ifndef _JAVA_JNI_SERVICEDIRECTORY_HPP_
#define _JAVA_JNI_SERVICEDIRECTORY_HPP_

#include <jni.h>

extern "C"
{
  JNIEXPORT jlong   Java_com_aldebaran_qimessaging_ServiceDirectory_qiTestSDCreate(JNIEnv *env, jobject obj);
  JNIEXPORT void    Java_com_aldebaran_qimessaging_ServiceDirectory_qiTestSDDestroy(jlong pServiceDirectory);
  JNIEXPORT jstring Java_com_aldebaran_qimessaging_ServiceDirectory_qiListenUrl(JNIEnv *env, jobject obj, jlong pServiceDirectory);
  JNIEXPORT void    Java_com_aldebaran_qimessaging_ServiceDirectory_qiTestSDClose(jlong pServiceDirectory);

} // !extern "C"

#endif // !_JAVA_JNI_SERVICEDIRECTORY_HPP_
