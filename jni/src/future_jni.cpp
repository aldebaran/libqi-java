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

void      java_future_callback(const qi::Future<qi::AnyReference>& future)
{
  JNIEnv *env = 0;
  jclass  cls = 0;

  // Attach JNIEnv to current thread to avoid segfault in jni functions. (eventloop dependent)
  if (JVM()->AttachCurrentThread((envPtr) &env, (void *) 0) != JNI_OK || env == 0)
  {
    qiLogError() << "Cannot attach callback thread to Java VM";
    throw std::runtime_error("Cannot attach callback thread to Java VM");
  }

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
  qi::Future<qi::AnyReference>* fut = reinterpret_cast<qi::Future<qi::AnyReference>*>(pFuture);

  if (fut->isCancelable() == false)
    return false;

  fut->cancel();
  return true;
}

jobject  Java_com_aldebaran_qimessaging_Future_qiFutureCallGet(JNIEnv *env, jobject obj, jlong pFuture)
{
  qi::Future<qi::AnyReference>* fut = reinterpret_cast<qi::Future<qi::AnyReference>*>(pFuture);

  try
  {
    // UGLY WORKAROUND
    // jobject typeinterface has deep-value meaning on the pointed _jobject
    // but jobject as a C++ type is just _jobject*
    // so workaround by allocating, and performin only a shallow delete
    qi::AnyReference arRes = fut->value();
    std::pair<qi::AnyReference, bool> converted = arRes.convert(qi::typeOf<jobject>());
    jobject result = * (jobject*)converted.first.rawValue();
    delete (jobject*)converted.first.rawValue();
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
  qi::Future<qi::AnyReference>* fut = reinterpret_cast<qi::Future<qi::AnyReference>*>(pFuture);

  qi::FutureState status = fut->wait(timeout);

  switch(status) {
  case qi::FutureState_FinishedWithValue:
    return Java_com_aldebaran_qimessaging_Future_qiFutureCallGetWithTimeout(env, obj, pFuture, 0);
  case qi::FutureState_Running:
    return 0;
  default:
    throwJavaError(env, fut->error().c_str());
  }

  return 0;
}

jboolean Java_com_aldebaran_qimessaging_Future_qiFutureCallIsCancelled(JNIEnv *env, jobject obj, jlong pFuture)
{
  qi::Future<qi::AnyReference>* fut = reinterpret_cast<qi::Future<qi::AnyReference>*>(pFuture);

  return fut->isCanceled();
}

jboolean Java_com_aldebaran_qimessaging_Future_qiFutureCallIsDone(JNIEnv *env, jobject obj, jlong pFuture)
{
  qi::Future<qi::AnyReference>* fut = reinterpret_cast<qi::Future<qi::AnyReference>*>(pFuture);

  return fut->isFinished();
}

jboolean Java_com_aldebaran_qimessaging_Future_qiFutureCallConnect(JNIEnv *env, jobject obj, jlong pFuture, jobject callback, jstring jclassName, jobjectArray args)
{
  qi::Future<qi::AnyReference>* fut = reinterpret_cast<qi::Future<qi::AnyReference>*>(pFuture);
  std::string className = qi::jni::toString(jclassName);
  qi::CallbackInfo* info = 0;

  // Attach JNIEnv to current thread to avoid segfault in jni functions. (eventloop dependent)
  if (JVM()->AttachCurrentThread((envPtr) &env, (void *) 0) != JNI_OK || env == 0)
  {
    qiLogError() << "Cannot attach callback thread to Java VM";
    throwJavaError(env, "Cannot attach callback thread to Java VM");
    return false;
  }

  info = new qi::CallbackInfo(callback, args, className);
  qi::FutureHandler::addCallbackInfo(fut, info);
  fut->connect(boost::bind(&java_future_callback, _1));
  return true;
}

void  Java_com_aldebaran_qimessaging_Future_qiFutureCallWaitWithTimeout(JNIEnv* QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pFuture, jint timeout)
{
  qi::Future<qi::AnyReference>* fut = reinterpret_cast<qi::Future<qi::AnyReference>*>(pFuture);

  if (timeout)
    fut->wait(timeout);
  else
    fut->wait();
}
