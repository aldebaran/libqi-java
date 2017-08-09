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
  JNIEXPORT jboolean JNICALL Java_com_aldebaran_qi_Future_qiFutureCallCancel(JNIEnv *env, jobject obj, jlong pFuture);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Future_qiFutureCallCancelRequest(JNIEnv *env, jobject obj, jlong pFuture);
  JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Future_qiFutureCallGet(JNIEnv *env, jobject obj, jlong pFuture, jint msecs);
  JNIEXPORT jstring JNICALL Java_com_aldebaran_qi_Future_qiFutureCallGetError(JNIEnv *env, jobject obj, jlong pFuture);
  JNIEXPORT jboolean JNICALL Java_com_aldebaran_qi_Future_qiFutureCallIsCancelled(JNIEnv *env, jobject obj, jlong pFuture);
  JNIEXPORT jboolean JNICALL Java_com_aldebaran_qi_Future_qiFutureCallIsDone(JNIEnv *env, jobject obj, jlong pFuture);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Future_qiFutureCallWaitWithTimeout(JNIEnv *env, jobject obj, jlong pFuture, jint timeout);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Future_qiFutureDestroy(JNIEnv* env, jobject obj, jlong pFuture);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Future_qiFutureCallConnectCallback(JNIEnv *env, jobject obj, jlong pFuture, jobject callback, jint futureCallbackType);
  /**
   * Call native future "andThen" for Function(P)->R
   * @param env JNI environment
   * @param thisFuture Caller object
   * @param pFuture Future pointer
   * @param function Function to callback
   * @return Created future
   */
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureAndThen(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject function);
  /**
   * Call native future "andThen" for Consumer(P)
   * @param env JNI environment
   * @param thisFuture Caller object
   * @param pFuture Future pointer
   * @param function Void function to callback
   * @return Created future
   */
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureAndThenVoid(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject function);
  /**
   * Call native future "andThen" for Function(P)->Future<R> with automatic unwrap
   * @param env JNI environment
   * @param thisFuture Caller object
   * @param pFuture Future pointer
   * @param function Function to callback
   * @return Created future
   */
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureAndThenUnwrap(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject function);
  /**
   * Call native future "then" for FutureFunction(Future<P>)->R
   * @param env JNI environment
   * @param thisFuture Caller object
   * @param pFuture Future pointer
   * @param futureFunction Function to callback
   * @return Created future
   */
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureThen(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject futureFunction);
  /**
   * Call native future "then" for and FutureFunction(Future<P>)->Void
   * @param env JNI environment
   * @param thisFuture Caller object
   * @param pFuture Future pointer
   * @param futureFunction Function to callback
   * @return Created future
   */
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureThenVoid(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject futureFunction);
  /**
   * Call native future "then" for Function(Future<P>)->Future<R> with automatic unwrap
   * @param env JNI environment
   * @param thisFuture Caller object
   * @param pFuture Future pointer
   * @param function Function to callback
   * @return Created future
   */
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureThenUnwrap(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject futureFunction);
} // !extern "C"

#endif //!_FUTURE_JNI_HPP_
