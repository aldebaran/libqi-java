/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
*/

#include <qitype/anyvalue.hpp>

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

  // Get Java information related to Java callback
  qi::CallbackInfo* info = qi::FutureHandler::methodInfo(future);
  if ((cls = env->FindClass(info->className.c_str())) == 0)
  {
    qiLogError() << "Cannot find com.aldebaran.qimessaging.Callback implementation";
    throw new std::runtime_error("Cannot find com.aldebaran.qimessaging.Callback interface");
  }

  if (future.hasError()) // Call onFailure
    qi::FutureHandler::onFailure(env, cls, info);

  if (!future.hasError() && future.isFinished()) // Call onSuccess
    qi::FutureHandler::onSuccess(env, cls, info);

  if (future.isFinished()) // Call onCompleted
    qi::FutureHandler::onComplete(env, cls, info);

  // Only called once, so remove and delete info
  qi::FutureHandler::removeCallbackInfo(future);
  env->DeleteLocalRef(cls);
}

jboolean  Java_com_aldebaran_qimessaging_Future_qiFutureCallCancel(JNIEnv *env, jobject obj, jlong pFuture, jboolean mayInterup)
{
  qi::Future<qi::AnyValue>* fut = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);

  if (fut->isCancelable() == false)
    return false;

  fut->cancel();
  return true;
}

jobject  Java_com_aldebaran_qimessaging_Future_qiFutureCallGet(JNIEnv *env, jobject obj, jlong pFuture)
{
  qi::Future<qi::AnyValue>* fut = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);

  try
  {
    qi::AnyReference arRes = fut->value().asReference();
    std::pair<qi::AnyReference, bool> converted = arRes.convert(qi::typeOf<jobject>());
    jobject result = * (jobject*)converted.first.rawValue();
    // keep it alive while we remove the global ref
    result = env->NewLocalRef(result);
    if (converted.second)
      converted.first.destroy();
    return result;
  }
  catch (std::runtime_error &e)
  {
    throwJavaError(env, e.what());
    return 0;
  }
}

jobject  Java_com_aldebaran_qimessaging_Future_qiFutureCallGetWithTimeout(JNIEnv *env, jobject obj, jlong pFuture, jint timeout)
{
  qiLogVerbose() << "Future wait " << timeout;
  qi::Future<qi::AnyValue>* fut = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);

  qi::FutureState status = fut->wait(timeout);
  qiLogVerbose() << "Waited, got " << status;
  switch(status) {
  case qi::FutureState_FinishedWithValue:
    return Java_com_aldebaran_qimessaging_Future_qiFutureCallGet(env, obj, pFuture);
  case qi::FutureState_Running:
    return 0;
  default:
    throwJavaError(env, fut->error().c_str());
  }

  return 0;
}

jboolean Java_com_aldebaran_qimessaging_Future_qiFutureCallIsCancelled(JNIEnv *env, jobject obj, jlong pFuture)
{
  qi::Future<qi::AnyValue>* fut = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);

  return fut->isCanceled();
}

jboolean Java_com_aldebaran_qimessaging_Future_qiFutureCallIsDone(JNIEnv *env, jobject obj, jlong pFuture)
{
  qi::Future<qi::AnyValue>* fut = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);

  return fut->isFinished();
}

jboolean Java_com_aldebaran_qimessaging_Future_qiFutureCallConnect(JNIEnv *env, jobject obj, jlong pFuture, jobject callback, jstring jclassName, jobjectArray args)
{
  qi::Future<qi::AnyValue>* fut = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);
  std::string className = qi::jni::toString(jclassName);
  qi::CallbackInfo* info = 0;

  qi::jni::JNIAttach attach(env);

  info = new qi::CallbackInfo(callback, args, className);
  qi::FutureHandler::addCallbackInfo(fut, info);
  fut->connect(boost::bind(&java_future_callback, _1));
  return true;
}

void  Java_com_aldebaran_qimessaging_Future_qiFutureCallWaitWithTimeout(JNIEnv* QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pFuture, jint timeout)
{
  qi::Future<qi::AnyValue>* fut = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);

  if (timeout)
    fut->wait(timeout);
  else
    fut->wait();
}

void  Java_com_aldebaran_qimessaging_Future_qiFutureDestroy(JNIEnv* QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pFuture)
{
  qi::Future<qi::AnyValue>* fut = reinterpret_cast<qi::Future<qi::AnyValue>*>(pFuture);
  delete fut;
}
