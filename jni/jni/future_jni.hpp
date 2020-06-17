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
#include <jni/jnitools.hpp>

/**
 * Constructs a Java qi.Future object from a C++ qi::Future.
 *
 * The Java qi.Future returned by this function matches the following conditions:
 *   Its value type is java/lang/Object.
 *   If T is itself a Future type, the result will be flattened.
 *
 * precondition: future shall be valid. It shall not have a struct/tupple parameter which signature is not known by libqi.
 *               The generic object (which signature is "o") cannot be used because AnyReferenceBase::convert does not manage this case.
 */
qi::jni::ScopedJObject<jobject> toJavaFuture(JNIEnv *env, qi::Future<qi::AnyValue> future);

template <typename T>
qi::jni::ScopedJObject<jobject> toJavaFuture(JNIEnv *env, qi::Future<T> future)
{
    return toJavaFuture(env, future.andThen([](const T& v){ return qi::AnyValue::from(v); }));
}

template <typename T>
qi::jni::ScopedJObject<jobject> toJavaFuture(JNIEnv *env, qi::Future<qi::Future<T>> future)
{
    // qi::Future<T> is recognised as a generic object, that is not managed by AnyReferenceBase::convert, so we chose to flatten the future
    // to avoid an error in that case.
    QI_ASSERT_TRUE(future.isValid());
    return toJavaFuture(env, future.unwrap());
}

qi::jni::ScopedJObject<jobject> toJavaFuture(JNIEnv *env, qi::Future<void> future);

/**
 * @brief obtainFutureCfromFutureJava Converts a Java Future into a C++ Future object.
 * @param env JNI environment
 * @param future Java Future
 * @return C future linked
 */
qi::Future<qi::AnyValue> toCppFuture(JNIEnv *env, jobject future);

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
  JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Future_qiFutureAndThen(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject function);
  /**
   * Call native future "andThen" for Consumer(P)
   * @param env JNI environment
   * @param thisFuture Caller object
   * @param pFuture Future pointer
   * @param function Void function to callback
   * @return Created future
   */
  JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Future_qiFutureAndThenVoid(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject function);
  /**
   * Call native future "andThen" for Function(P)->Future<R> with automatic unwrap
   * @param env JNI environment
   * @param thisFuture Caller object
   * @param pFuture Future pointer
   * @param function Function to callback
   * @return Created future
   */
  JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Future_qiFutureAndThenUnwrap(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject function);
  /**
   * Call native future "then" for FutureFunction(Future<P>)->R
   * @param env JNI environment
   * @param thisFuture Caller object
   * @param pFuture Future pointer
   * @param futureFunction Function to callback
   * @return Created future
   */
  JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Future_qiFutureThen(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject futureFunction);
  /**
   * Call native future "then" for and FutureFunction(Future<P>)->Void
   * @param env JNI environment
   * @param thisFuture Caller object
   * @param pFuture Future pointer
   * @param futureFunction Function to callback
   * @return Created future
   */
  JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Future_qiFutureThenVoid(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject futureFunction);
  /**
   * Call native future "then" for Function(Future<P>)->Future<R> with automatic unwrap
   * @param env JNI environment
   * @param thisFuture Caller object
   * @param pFuture Future pointer
   * @param function Function to callback
   * @return Created future
   */
  JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Future_qiFutureThenUnwrap(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject futureFunction);
} // !extern "C"

#endif //!_FUTURE_JNI_HPP_
