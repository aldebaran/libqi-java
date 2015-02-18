/*
**
** Author(s):
**  - Philippe Daouadi <pdaouadi@aldebaran.com>
**
** Copyright (C) 2015 Aldebaran Robotics
*/

#ifndef _JAVA_JNI_RAW_APPLICATION_HPP_
#define _JAVA_JNI_RAW_APPLICATION_HPP_

#include <jni.h>

extern "C"
{
  JNIEXPORT jlong Java_com_aldebaran_qi_RawApplication_qiApplicationCreate(JNIEnv *env, jclass QI_UNUSED(jobj), jobjectArray jargs, jstring jdefaultUrl, jboolean listen);
  JNIEXPORT void  Java_com_aldebaran_qi_RawApplication_qiApplicationDestroy(JNIEnv *env, jclass, jlong pApplication);
  JNIEXPORT void  Java_com_aldebaran_qi_RawApplication_qiApplicationRun(JNIEnv *env, jclass, jlong pApplication);
  JNIEXPORT void  Java_com_aldebaran_qi_RawApplication_qiApplicationStop(JNIEnv *env, jclass, jlong pApplication);
} // !extern "C"

#endif // !_JAVA_JNI_APPLICATION_HPP_
