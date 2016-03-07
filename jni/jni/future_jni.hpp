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
#include <qi/future.hpp>

//jobject   newJavaFuture(qi::Future<qi::AnyValue> *fut);

extern "C"
{
  JNIEXPORT jboolean Java_com_aldebaran_qi_Future_qiFutureCallCancel(JNIEnv *env, jobject obj, jlong pFuture, jboolean mayInterup);
  JNIEXPORT jobject  Java_com_aldebaran_qi_Future_qiFutureCallGet(JNIEnv *env, jobject obj, jlong pFuture);
  JNIEXPORT jobject  Java_com_aldebaran_qi_Future_qiFutureCallGetWithTimeout(JNIEnv *env, jobject obj, jlong pFuture, jint timeout);
  JNIEXPORT jstring  Java_com_aldebaran_qi_Future_qiFutureCallGetError(JNIEnv *env, jobject obj, jlong pFuture);
  JNIEXPORT jboolean Java_com_aldebaran_qi_Future_qiFutureCallIsCancelled(JNIEnv *env, jobject obj, jlong pFuture);
  JNIEXPORT jboolean Java_com_aldebaran_qi_Future_qiFutureCallIsDone(JNIEnv *env, jobject obj, jlong pFuture);
  JNIEXPORT jboolean Java_com_aldebaran_qi_Future_qiFutureCallConnect(JNIEnv *env, jobject obj, jlong pFuture, jobject callable, jstring className, jobjectArray args);
  JNIEXPORT void     Java_com_aldebaran_qi_Future_qiFutureCallWaitWithTimeout(JNIEnv *env, jobject obj, jlong pFuture, jint timeout);
  JNIEXPORT void     Java_com_aldebaran_qi_Future_qiFutureDestroy(JNIEnv* QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pFuture);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Future_qiFutureCallConnectCallback(JNIEnv *env, jobject obj, jlong pFuture, jobject callback);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureCallThen(JNIEnv *env, jobject obj, jlong pFuture, jobject qiFunction);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureCallAndThen(JNIEnv *env, jobject obj, jlong pFuture, jobject qiFunction);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureCreate(JNIEnv *env, jclass cls, jobject value);
} // !extern "C"

#endif //!_FUTURE_JNI_HPP_
