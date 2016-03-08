/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#ifndef _FUTURE_JNI_HPP_
#define _FUTURE_JNI_HPP_

#include <jni.h>

extern "C"
{
  JNIEXPORT jboolean JNICALL Java_com_aldebaran_qi_Future_qiFutureCallCancel(JNIEnv *env, jobject obj, jlong pFuture, jboolean mayInterrupt);
  JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Future_qiFutureCallGet(JNIEnv *env, jobject obj, jlong pFuture);
  JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Future_qiFutureCallGetWithTimeout(JNIEnv *env, jobject obj, jlong pFuture, jint timeout);
  JNIEXPORT jstring JNICALL Java_com_aldebaran_qi_Future_qiFutureCallGetError(JNIEnv *env, jobject obj, jlong pFuture);
  JNIEXPORT jboolean JNICALL Java_com_aldebaran_qi_Future_qiFutureCallIsCancelled(JNIEnv *env, jobject obj, jlong pFuture);
  JNIEXPORT jboolean JNICALL Java_com_aldebaran_qi_Future_qiFutureCallIsDone(JNIEnv *env, jobject obj, jlong pFuture);
  JNIEXPORT jboolean JNICALL Java_com_aldebaran_qi_Future_qiFutureCallConnect(JNIEnv *env, jobject obj, jlong pFuture, jobject callable, jstring className, jobjectArray args);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Future_qiFutureCallWaitWithTimeout(JNIEnv *env, jobject obj, jlong pFuture, jint timeout);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Future_qiFutureDestroy(JNIEnv* env, jobject obj, jlong pFuture);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Future_qiFutureCallConnectCallback(JNIEnv *env, jobject obj, jlong pFuture, jobject callback);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureCallThen(JNIEnv *env, jobject obj, jlong pFuture, jobject qiFunction);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureCallAndThen(JNIEnv *env, jobject obj, jlong pFuture, jobject qiFunction);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureCreate(JNIEnv *env, jclass cls, jobject value);
} // !extern "C"

#endif //!_FUTURE_JNI_HPP_
