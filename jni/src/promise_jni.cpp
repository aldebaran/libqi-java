#include "promise_jni.hpp"

#include <qi/future.hpp>
#include "jnitools.hpp"

qiLogCategory("qimessaging.java");

/*
 * Class:     com_aldebaran_qi_Promise
 * Method:    _newPromise
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Promise__1newPromise
  (JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj))
{
  auto promisePtr = new qi::Promise<qi::AnyValue>();
  return reinterpret_cast<jlong>(promisePtr);
}

/*
 * Class:     com_aldebaran_qi_Promise
 * Method:    _getFuture
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Promise__1getFuture
  (JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong promisePtr)
{
  auto promise = reinterpret_cast<qi::Promise<qi::AnyValue> *>(promisePtr);
  auto futurePtr = new qi::Future<qi::AnyValue>{ promise->future() };
  return reinterpret_cast<jlong>(futurePtr);
}

/*
 * Class:     com_aldebaran_qi_Promise
 * Method:    _setValue
 * Signature: (JLjava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_com_aldebaran_qi_Promise__1setValue
  (JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong promisePtr, jobject value)
{
  auto promise = reinterpret_cast<qi::Promise<qi::AnyValue> *>(promisePtr);
  promise->setValue(qi::AnyValue::from<jobject>(value));
}

/*
 * Class:     com_aldebaran_qi_Promise
 * Method:    _setError
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_aldebaran_qi_Promise__1setError
  (JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong promisePtr, jstring error)
{
  auto promise = reinterpret_cast<qi::Promise<qi::AnyValue> *>(promisePtr);
  promise->setError(qi::jni::toString(error));
}

/*
 * Class:     com_aldebaran_qi_Promise
 * Method:    _setCanceled
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_aldebaran_qi_Promise__1setCancelled
  (JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong promisePtr)
{
  auto promise = reinterpret_cast<qi::Promise<qi::AnyValue> *>(promisePtr);
  // "cancelled" in Java, "canceled" in libqi
  promise->setCanceled();
}
