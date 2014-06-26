/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
*/

#ifndef _FUTUREHANDLER_HPP_
#define _FUTUREHANDLER_HPP_

#include <jni.h>
#include <qi/anyobject.hpp>
#include <qi/type/dynamicobjectbuilder.hpp>
#include <qi/anyfunction.hpp>

#include <jnitools.hpp>

namespace qi
{
  /**
   * @brief The CallbackInfo struct Container used by FutureHandler
   */
  struct CallbackInfo
  {
    jobject      instance;
    jobjectArray args;
    std::string  className;
    std::string  interfaceName;

    CallbackInfo(jobject instance, jobjectArray args, const std::string& className, const std::string& interfaceName = "com/aldebaran/qimessaging/Callback")
    {
      JNIEnv* env = 0;
      JVM()->GetEnv((void**) &env, QI_JNI_MIN_VERSION);

      // We need to get global reference on each object of array to use them in callback thread.
      jint size = env->GetArrayLength(args);
      jobjectArray array = (jobjectArray) env->NewGlobalRef(env->NewObjectArray(size, env->FindClass("java/lang/Object"), 0));
      jint i = 0;
      while (i < size)
      {
        jobject current = env->GetObjectArrayElement(args, i);
        env->SetObjectArrayElement(array, i, env->NewGlobalRef(current));
        i++;
      }

      this->instance = env->NewGlobalRef(instance);
      this->args = array;
      this->className = className;
      this->interfaceName = interfaceName;
    }

    ~CallbackInfo()
    {
      JNIEnv* env = 0;
      JVM()->GetEnv((void**) &env, QI_JNI_MIN_VERSION);

      // Destroy all global reference on arguments array
      jint size = env->GetArrayLength(this->args);
      jint i = 0;
      while (i < size)
      {
        jobject current = env->GetObjectArrayElement(this->args, i);
        env->DeleteGlobalRef(current);
        i++;
      }

      env->DeleteGlobalRef(this->args);
      env->DeleteGlobalRef(this->instance);
    }
  };

  /**
   * @brief The FutureHandler class Helper class bridging C++ Future and Java callback (onSuccess, onFailure, onComplete) implementation
   */
  class FutureHandler
  {
    public:
      static void onSuccess(JNIEnv *env, jclass cls, qi::CallbackInfo *info);
      static void onFailure(JNIEnv *env, jclass cls, qi::CallbackInfo *info);
      static void onComplete(JNIEnv *env, jclass cls, qi::CallbackInfo *info);
      static qi::CallbackInfo* methodInfo(const qi::Future<qi::AnyValue> &future);
      static void addCallbackInfo(qi::Future<AnyValue>* future, qi::CallbackInfo* info);
      static void removeCallbackInfo(const qi::Future<qi::AnyValue>& future);
      static jobject futurePointer(JNIEnv* env, qi::CallbackInfo* info);

    public:
      std::map<qi::Future<qi::AnyValue>*, qi::CallbackInfo*> _map;
  };

} // !qi

#endif // !_FUTUREHANDLER_HPP_
