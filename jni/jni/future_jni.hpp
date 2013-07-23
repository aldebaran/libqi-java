/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
*/

#ifndef _FUTURE_JNI_HPP_
#define _FUTURE_JNI_HPP_

#include <jni.h>
#include <qi/future.hpp>

//jobject   newJavaFuture(qi::Future<qi::AnyReference> *fut);

extern "C"
{
  JNIEXPORT jboolean Java_com_aldebaran_qimessaging_Future_qiFutureCallCancel(JNIEnv *env, jobject obj, jlong pFuture, jboolean mayInterup);
  JNIEXPORT jobject  Java_com_aldebaran_qimessaging_Future_qiFutureCallGet(JNIEnv *env, jobject obj, jlong pFuture);
  JNIEXPORT jobject  Java_com_aldebaran_qimessaging_Future_qiFutureCallGetWithTimeout(JNIEnv *env, jobject obj, jlong pFuture, jint timeout);
  JNIEXPORT jboolean Java_com_aldebaran_qimessaging_Future_qiFutureCallIsCancelled(JNIEnv *env, jobject obj, jlong pFuture);
  JNIEXPORT jboolean Java_com_aldebaran_qimessaging_Future_qiFutureCallIsDone(JNIEnv *env, jobject obj, jlong pFuture);
  JNIEXPORT jboolean Java_com_aldebaran_qimessaging_Future_qiFutureCallConnect(JNIEnv *env, jobject obj, jlong pFuture, jobject callable, jstring className, jobjectArray args);
  JNIEXPORT void     Java_com_aldebaran_qimessaging_Future_qiFutureCallWaitWithTimeout(JNIEnv *env, jobject obj, jlong pFuture, jint timeout);
} // !extern "C"

#endif //!_FUTURE_JNI_HPP_
