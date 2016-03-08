/*
**
** Author(s):
**  - Philippe Daouadi <pdaouadi@aldebaran.com>
**
** Copyright (C) 2015 Aldebaran Robotics
** See COPYING for the license
*/

#ifndef _JAVA_JNI_RAW_APPLICATION_HPP_
#define _JAVA_JNI_RAW_APPLICATION_HPP_

#include <jni.h>

extern "C"
{
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_RawApplication_qiApplicationCreate(JNIEnv *env, jobject obj, jobjectArray jargs, jstring jdefaultUrl, jboolean listen);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_RawApplication_qiApplicationDestroy(JNIEnv *env, jobject obj, jlong pApplication);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_RawApplication_qiApplicationRun(JNIEnv *env, jobject obj, jlong pApplication);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_RawApplication_qiApplicationStop(JNIEnv *env, jobject obj, jlong pApplication);
} // !extern "C"

#endif // !_JAVA_JNI_APPLICATION_HPP_
