/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#ifndef _JAVA_JNI_APPLICATION_HPP_
#define _JAVA_JNI_APPLICATION_HPP_

#include <jni.h>

jlong createApplication(JNIEnv* env, jobjectArray jargs, const boost::function<jlong(int& argc, char**& argv)>& fn);

extern "C"
{
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Application_qiApplicationCreate(JNIEnv *env, jobject obj, jobjectArray jargs, jstring jdefaultUrl, jboolean listen);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Application_qiApplicationGetSession(JNIEnv *env, jobject obj, jlong pApplication);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Application_qiApplicationDestroy(JNIEnv *env, jobject obj, jlong pApplication);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Application_qiApplicationStart(JNIEnv *env, jobject obj, jlong pApplication);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Application_qiApplicationRun(JNIEnv *env, jobject obj, jlong pApplication);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Application_qiApplicationStop(JNIEnv *env, jobject obj, jlong pApplication);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Application_setLogCategory(JNIEnv *env, jclass cls, jstring category, jlong verbosity);
} // !extern "C"

#endif // !_JAVA_JNI_APPLICATION_HPP_
