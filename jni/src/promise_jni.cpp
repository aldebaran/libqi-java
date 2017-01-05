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
  (JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jint futureCallbackType)
{
  qi::FutureCallbackType type = static_cast<qi::FutureCallbackType>(futureCallbackType);
  auto promisePtr = new qi::Promise<qi::AnyValue>(type);
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

namespace
{
  void jni_invoke_cancel_callback(qi::jni::SharedGlobalRef callback, qi::jni::SharedGlobalRef args )
  {
      const char *method = "onCancelRequested";
      const char *methodSig = "(Lcom/aldebaran/qi/Promise;)V";
      qi::jni::JNIAttach attach;
      JNIEnv *env = attach.get();
      qi::jni::Call<void>::invoke(env, callback.get(), method, methodSig, args.get());
  }

}

/*
 * Class:     com_aldebaran_qi_Promise
 * Method:    _setOnCancel
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_aldebaran_qi_Promise__1setOnCancel(JNIEnv *env, jobject thisPromise, jlong promisePtr, jobject callback)
{
  auto promise = reinterpret_cast<qi::Promise<qi::AnyValue> *>(promisePtr);
  auto gCallback = qi::jni::makeSharedGlobalRef(env, callback);
  auto gThisPromise = qi::jni::makeSharedGlobalRef(env, thisPromise);
  promise->setOnCancel( [=](qi::Promise<qi::AnyValue> p){
      jni_invoke_cancel_callback(gCallback, gThisPromise);
  } );

}
