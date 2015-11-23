/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#include <futurehandler.hpp>

/**
 * @brief globalFutureHandler
 * Foreach callback set in Java, an entry is added here.
 * QiMessaging bindings search here when callback from future is called to find out which Java callback
 * needs to be called.
 */
static qi::FutureHandler globalFutureHandler;

namespace qi {

  void FutureHandler::onSuccess(JNIEnv *env, jclass cls, qi::CallbackInfo *info)
  {
    jmethodID mid = env->GetMethodID(cls, "onSuccess", "(Lcom/aldebaran/qi/Future;[Ljava/lang/Object;)V");
    if (mid == 0)
    {
      qiLogError("qimessaging.jni") << "onSuccess method of com.aldebaran.qi.Callback is not implemented";
      return;
    }

    jobject fut = FutureHandler::futurePointer(env, info);
    env->CallVoidMethod(info->instance, mid, fut, info->args);
    env->DeleteGlobalRef(fut);
  }

  void FutureHandler::onFailure(JNIEnv *env, jclass cls, qi::CallbackInfo *info)
  {
    jmethodID mid = env->GetMethodID(cls, "onFailure", "(Lcom/aldebaran/qi/Future;[Ljava/lang/Object;)V");
    if (mid == 0)
    {
      qiLogError("qimessaging.jni") << "onFailure method of com.aldebaran.qi.Callback is not implemented";
      return;
    }

    jobject fut = FutureHandler::futurePointer(env, info);
    env->CallVoidMethod(info->instance, mid, fut, info->args);
    env->DeleteGlobalRef(fut);
  }

  void FutureHandler::onComplete(JNIEnv *env, jclass cls, qi::CallbackInfo *info)
  {
    jmethodID mid = env->GetMethodID(cls, "onComplete", "(Lcom/aldebaran/qi/Future;[Ljava/lang/Object;)V");
    if (mid == 0)
    {
      qiLogError("qimessaging.jni") << "onComplete method of com.aldebaran.qi.Callback is not implemented";
      return;
    }

    jobject fut = FutureHandler::futurePointer(env, info);
    env->CallVoidMethod(info->instance, mid, fut, info->args);
    env->DeleteGlobalRef(fut);
  }

  qi::CallbackInfo* FutureHandler::methodInfo(const qi::Future<qi::AnyValue> &future)
  {
    for (std::map<qi::Future<qi::AnyValue>*, qi::CallbackInfo*>::iterator it = globalFutureHandler._map.begin();
         it != globalFutureHandler._map.end(); ++it)
    {
      qi::Future<qi::AnyValue> *iterator = (*it).first;

      if (*iterator == future)
        return (*it).second;
    }

    return 0;
  }

  void FutureHandler::addCallbackInfo(qi::Future<AnyValue> *future, qi::CallbackInfo* info)
  {
    globalFutureHandler._map[future] = info;
  }

  void FutureHandler::removeCallbackInfo(const qi::Future<qi::AnyValue>& future)
  {
    for (std::map<qi::Future<qi::AnyValue>*, qi::CallbackInfo*>::iterator it = globalFutureHandler._map.begin();
         it != globalFutureHandler._map.end(); ++it)
    {
      qi::Future<qi::AnyValue> *iterator = (*it).first;

      if (*iterator == future)
      {
        delete (*it).second;
        globalFutureHandler._map.erase(it);
        return;
      }
    }
  }

  jobject FutureHandler::futurePointer(JNIEnv *env, qi::CallbackInfo* info)
  {
    for (std::map<qi::Future<qi::AnyValue>*, qi::CallbackInfo*>::iterator it = globalFutureHandler._map.begin();
         it != globalFutureHandler._map.end(); ++it)
    {
      if ((*it).second == info)
      {
        jclass futureCls = 0;
        jmethodID init = 0;

        futureCls = qi::jni::clazz("Future");
        if (!futureCls)
        {
          qiLogError("qimessaging.jni") << "Cannot find com.aldebaran.qi.Future class";
          return 0;
        }

        if ((init = env->GetMethodID(futureCls, "<init>", "(J)V")) == 0)
        {
          qiLogError("qimessaging.jni") << "Cannot find com.aldebaran.qi.Future.<init>(J) constructor";
          return 0;
        }

        // Create a new Future class.
        // Add a new global ref to object to avoid destruction before entry into Java code.
        jobject future = env->NewObject(futureCls, init, (*it).first);
#ifdef ANDROID
        return env->NewGlobalRef(future);
#else
        env->NewGlobalRef(future);
        return future;
#endif
      }
    }

    return 0;
  }

} // !qi
