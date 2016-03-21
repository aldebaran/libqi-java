/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#include <qi/anyvalue.hpp>

#include <jnitools.hpp>
#include <futurehandler.hpp>
#include <future_jni.hpp>
#include <callbridge.hpp>

qiLogCategory("qimessaging.java");

void      java_future_callback(const qi::Future<qi::AnyValue>& future)
{
  JNIEnv *env = 0;
  jclass  cls = 0;

  qi::jni::JNIAttach attach;
  env = attach.get();

  qi::CallbackInfo* info = qi::FutureHandler::methodInfo(future);
  cls =  info->clazz;

  if (future.hasError()) // Call onFailure
    qi::FutureHandler::onFailure(env, cls, info);

  if (!future.hasError() && future.isFinished()) // Call onSuccess
    qi::FutureHandler::onSuccess(env, cls, info);

  if (future.isFinished()) // Call onCompleted
    qi::FutureHandler::onComplete(env, cls, info);

  // Only called once, so remove and delete info
  qi::FutureHandler::removeCallbackInfo(future);
}

JNIEXPORT jboolean JNICALL Java_com_aldebaran_qi_Future_qiFutureCallCancel(JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pFuture) {
  qi::Future<qi::AnyValue>* fut = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);

  if (fut->isCancelable() == false)
    return false;

  fut->cancel();
  return true;
}

JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Future_qiFutureCallGet(JNIEnv *env, jobject QI_UNUSED(obj), jlong pFuture, jint msecs)
{
  qi::Future<qi::AnyValue>* fut = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);

  try
  {
    int qiMsecs = msecs == -1 ? qi::FutureTimeout_Infinite : msecs;
    qi::AnyReference arRes = fut->value(qiMsecs).asReference();
    std::pair<qi::AnyReference, bool> converted = arRes.convert(qi::typeOf<jobject>());
    jobject result = * (jobject*)converted.first.rawValue();
    // keep it alive while we remove the global ref
    result = env->NewLocalRef(result);
    if (converted.second)
      converted.first.destroy();
    return result;
  }
  catch (const qi::FutureException &e)
  {
    switch (e.state())
    {
    case qi::FutureException::ExceptionState_FutureTimeout:
      throwNewTimeoutException(env, "native future timeout");
      break;
    case qi::FutureException::ExceptionState_FutureCanceled:
      // libqi uses "canceled"/"cancelation" while java uses "cancelled"/"cancellation"...
      throwNewCancellationException(env, "native future cancelled");
      break;
    case qi::FutureException::ExceptionState_FutureUserError:
      throwNewExecutionException(env, e.what());
      break;
    default:
      // unexpected error => java RuntimeException
      throwNewRuntimeException(env, e.what());
    }
    return nullptr;
  }
  catch (std::runtime_error &e)
  {
    // unexpected error => java RuntimeException
    throwNewRuntimeException(env, e.what());
    return nullptr;
  }
}

JNIEXPORT jboolean JNICALL Java_com_aldebaran_qi_Future_qiFutureCallIsCancelled(JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pFuture)
{
  qi::Future<qi::AnyValue>* fut = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);

  return fut->isCanceled();
}

JNIEXPORT jboolean JNICALL Java_com_aldebaran_qi_Future_qiFutureCallIsDone(JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pFuture)
{
  qi::Future<qi::AnyValue>* fut = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);

  return fut->isFinished();
}

JNIEXPORT jboolean JNICALL Java_com_aldebaran_qi_Future_qiFutureCallConnect(JNIEnv *env, jobject QI_UNUSED(obj), jlong pFuture, jobject callback, jstring jclassName, jobjectArray args)
{
  qi::Future<qi::AnyValue>* fut = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);
  std::string className = qi::jni::toString(jclassName);
  qi::CallbackInfo* info = 0;

  qi::jni::JNIAttach attach(env);
  jclass clazz = env->FindClass(className.c_str());
  info = new qi::CallbackInfo(callback, args, clazz);
  qi::FutureHandler::addCallbackInfo(fut, info);
  fut->connect(boost::bind(&java_future_callback, _1));
  return true;
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_Future_qiFutureCallWaitWithTimeout(JNIEnv* QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pFuture, jint timeout)
{
  qi::Future<qi::AnyValue>* fut = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);

  if (timeout)
    fut->wait(timeout);
  else
    fut->wait();
}

struct CallbackFunctor
{
  qi::jni::SharedGlobalRef _argFuture;
  qi::jni::SharedGlobalRef _callback;
  void operator()(const qi::Future<qi::AnyValue> &future) const;
};

JNIEXPORT void JNICALL Java_com_aldebaran_qi_Future_qiFutureCallConnectCallback(JNIEnv *env, jobject thisFuture, jlong pFuture, jobject callback)
{
  qi::Future<qi::AnyValue>* future = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);
  auto gThisFuture = qi::jni::makeSharedGlobalRef(env, thisFuture);
  auto gCallback = qi::jni::makeSharedGlobalRef(env, callback);
  future->connect(CallbackFunctor{ gThisFuture, gCallback });
}

template <typename Param>
struct QiFunctionFunctor
{
  qi::jni::SharedGlobalRef _argFuture;
  qi::jni::SharedGlobalRef _qiFunction;
  qi::Future<qi::AnyValue> operator()(Param) const;
};
using ThenFunctor = QiFunctionFunctor<qi::Future<qi::AnyValue>>;
using AndThenFunctor = QiFunctionFunctor<qi::AnyValue>;

JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureCallThen(JNIEnv *env, jobject thisFuture, jlong pFuture, jobject qiFunction)
{
  qi::jni::JNIAttach attach(env);
  qi::Future<qi::AnyValue>* future = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);
  auto gThisFuture = qi::jni::makeSharedGlobalRef(env, thisFuture);
  auto gQiFunction = qi::jni::makeSharedGlobalRef(env, qiFunction);
  ThenFunctor functor{ gThisFuture, gQiFunction };
  auto result = new qi::Future<qi::AnyValue>{ future->then(functor).unwrap() };
  return reinterpret_cast<jlong>(result);
}

JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureCallAndThen(JNIEnv *env, jobject thisFuture, jlong pFuture, jobject qiFunction)
{
  qi::jni::JNIAttach attach(env);
  qi::Future<qi::AnyValue>* future = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);
  auto gThisFuture = qi::jni::makeSharedGlobalRef(env, thisFuture);
  auto gQiFunction = qi::jni::makeSharedGlobalRef(env, qiFunction);
  AndThenFunctor functor{ gThisFuture, gQiFunction };
  auto result = new qi::Future<qi::AnyValue>{ future->andThen(functor).unwrap() };
  return reinterpret_cast<jlong>(result);
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_Future_qiFutureDestroy(JNIEnv* QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pFuture)
{
  qi::Future<qi::AnyValue>* fut = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);
  delete fut;
}

JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureCreate(JNIEnv *env, jclass cls, jobject value)
{
    auto future = new qi::Future<qi::AnyValue>(qi::AnyValue::from<jobject>(value));
    return reinterpret_cast<jlong>(future);
}

void CallbackFunctor::operator()(const qi::Future<qi::AnyValue> &QI_UNUSED(future)) const
{
  // we stored the jobject future, so we ignore the parameter

  jobject argFuture = _argFuture.get();
  jobject callback = _callback.get();

  qi::jni::JNIAttach attach;
  JNIEnv *env = attach.get();

  const char *method = "onFinished";
  const char *methodSig = "(Lcom/aldebaran/qi/Future;)V";
  qi::jni::Call<void>::invoke(env, callback, method, methodSig, argFuture);
  if (env->ExceptionCheck() == JNI_TRUE) {
    qiLogError() << "Cannot call Future.Callback.onFinished(…) from JNI";
  }
}

template <typename Param>
qi::Future<qi::AnyValue> QiFunctionFunctor<Param>::operator()(Param) const
{
  // We ignore the parameter in order to use the same QiFunction for both
  // and() and andThen(), while the parameter differ in C++.
  // Instead, we always use the original future "argFuture".

  jobject argFuture = _argFuture.get();
  jobject qiFunction = _qiFunction.get();

  qi::jni::JNIAttach attach;
  JNIEnv *env = attach.get();

  // call QiFunction
  const char *method = "execute";
  const char *methodSig = "(Lcom/aldebaran/qi/Future;)Lcom/aldebaran/qi/Future;";
  jobject future = qi::jni::Call<jobject>::invoke(env, qiFunction, method, methodSig, argFuture);
  if (env->ExceptionCheck() == JNI_TRUE) {
    qiLogError() << "Cannot call QiFunction.execute(…) from JNI";
    return {};
  }
  if (!future) {
    throwNewNullPointerException(env, "QiFunction.execute() returned null");
    return {};
  }

  jlong pFuture = qi::jni::getField<jlong>(env, future, "_fut");
  if (env->ExceptionCheck() == JNI_TRUE) {
    qiLogError() << "Field not found: Future._fut";
    return {};
  }

  return *reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);
}
