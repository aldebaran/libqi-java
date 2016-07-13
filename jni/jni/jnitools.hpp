/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#ifndef _JAVA_JNI_JNITOOLS_HPP_
#define _JAVA_JNI_JNITOOLS_HPP_

#include <iostream>
#include <jni.h>

#ifdef ANDROID
# include <android/log.h>
#endif

// Define a portable JNIEnv* pointer (API between arm and intel differs)
#ifdef ANDROID
  typedef JNIEnv** envPtr;
#else
  typedef void** envPtr;
#endif

// Define JNI minimum version used by qimessaging bindings
#define QI_JNI_MIN_VERSION JNI_VERSION_1_6
// QI_OBJECT_CLASS defines complete name of java generic object class
#define QI_OBJECT_CLASS "com/aldebaran/qi/AnyObject"

extern jclass cls_string;
extern jclass cls_integer;
extern jclass cls_float;
extern jclass cls_double;
extern jclass cls_long;
extern jclass cls_boolean;

extern jclass cls_future;
extern jclass cls_anyobject;
extern jclass cls_tuple;

extern jclass cls_list;
extern jclass cls_arraylist;

extern jclass cls_map;
extern jclass cls_hashmap;

// JNI utils
extern "C"
{
  JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void*);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_EmbeddedTools_initTypeSystem(JNIEnv* env, jclass cls);
} // !extern C


namespace qi {
  class AnyReference;
  namespace jni {

    class JNIAttach
    {
      public:
        JNIAttach(JNIEnv* env = 0);
        ~JNIAttach();

        JNIEnv* get();
    };

    // String conversion
    std::string toString(jstring input);
    jstring     toJstring(const std::string& input);
    void        releaseString(jstring input);
    // TypeSystem tools
    jclass      clazz(jobject object);
    void        releaseClazz(jclass clazz);
    // JVM Environment management
    JNIEnv*     env();
    void        releaseObject(jobject obj);
    // Signature
    std::string javaSignature(const std::string& qiSignature);
    std::string qiSignature(jclass clazz);
    jobjectArray toJobjectArray(const std::vector<AnyReference> &values);

    template<typename R>
    struct Call
    {
      template <typename... Types>
      static R invoke(JNIEnv*     env,
                      jobject     jobject,
                      const char* methodName,
                      const char* methodSig,
                      Types       ...rest);
    };

    template <>
    template <typename... Types>
    void Call<void>::invoke(JNIEnv*     env,
                            jobject     jobject,
                            const char* methodName,
                            const char* methodSig,
                            Types       ...rest)
    {
      jclass cls = env->GetObjectClass(jobject);
      jmethodID mid = env->GetMethodID(cls, methodName, methodSig);
      env->DeleteLocalRef(cls);
      return env->CallVoidMethod(jobject, mid, rest...);
    }

    template <>
    template <typename... Types>
    jboolean Call<jboolean>::invoke(JNIEnv*     env,
                                    jobject     jobject,
                                    const char* methodName,
                                    const char* methodSig,
                                    Types       ...rest)
    {
      jclass cls = env->GetObjectClass(jobject);
      jmethodID mid = env->GetMethodID(cls, methodName, methodSig);
      env->DeleteLocalRef(cls);
      return env->CallBooleanMethod(jobject, mid, rest...);
    }

    template <>
    template <typename... Types>
    jobject Call<jobject>::invoke(JNIEnv*     env,
                                  jobject     jobject,
                                  const char* methodName,
                                  const char* methodSig,
                                  Types       ...rest)
    {
      jclass cls = env->GetObjectClass(jobject);
      jmethodID mid = env->GetMethodID(cls, methodName, methodSig);
      env->DeleteLocalRef(cls);
      return env->CallObjectMethod(jobject, mid, rest...);
    }

    template <typename R>
    using JNIFieldGetter = R (JNIEnv::*)(jobject, jfieldID);

    template <typename R, JNIFieldGetter<R> GETTER>
    R _getField(JNIEnv *env, jobject obj, const char *name, const char *sig)
    {
      jclass cls = env->GetObjectClass(obj);
      jfieldID fid = env->GetFieldID(cls, name, sig);
      env->DeleteLocalRef(cls);
      if (!fid) {
        // the caller must check any pending exception
        return {};
      }
      return (env->*GETTER)(obj, fid);
    }

    inline jobject getObjectField(JNIEnv *env, jobject obj, const char *name, const char *sig)
    {
      return _getField<jobject, &JNIEnv::GetObjectField>(env, obj, name, sig);
    }

    template <typename R>
    R getField(JNIEnv *env, jobject jobject, const char *name);

    template <>
    inline jboolean getField(JNIEnv *env, jobject obj, const char *name)
    {
      return _getField<jboolean, &JNIEnv::GetBooleanField>(env, obj, name, "Z");
    }

    template <>
    inline jbyte getField(JNIEnv *env, jobject obj, const char *name)
    {
      return _getField<jbyte, &JNIEnv::GetByteField>(env, obj, name, "B");
    }

    template <>
    inline jchar getField(JNIEnv *env, jobject obj, const char *name)
    {
      return _getField<jchar, &JNIEnv::GetCharField>(env, obj, name, "C");
    }

    template <>
    inline jshort getField(JNIEnv *env, jobject obj, const char *name)
    {
      return _getField<jshort, &JNIEnv::GetShortField>(env, obj, name, "S");
    }

    template <>
    inline jint getField(JNIEnv *env, jobject obj, const char *name)
    {
      return _getField<jint, &JNIEnv::GetIntField>(env, obj, name, "I");
    }

    template <>
    inline jlong getField(JNIEnv *env, jobject obj, const char *name)
    {
      return _getField<jlong, &JNIEnv::GetLongField>(env, obj, name, "J");
    }

    template <>
    inline jfloat getField(JNIEnv *env, jobject obj, const char *name)
    {
      return _getField<jfloat, &JNIEnv::GetFloatField>(env, obj, name, "F");
    }

    template <>
    inline jdouble getField(JNIEnv *env, jobject obj, const char *name)
    {
      return _getField<jdouble, &JNIEnv::GetDoubleField>(env, obj, name, "D");
    }

    template <typename ...T>
    inline jobject construct(JNIEnv *env, const char *className, const char *sig, T... params)
    {
      jclass cls = env->FindClass(className);
      if (!cls)
        return nullptr;

      jmethodID ctor = env->GetMethodID(cls, "<init>", sig);
      if (!ctor) {
        env->DeleteLocalRef(cls);
        return nullptr;
      }

      jobject result = env->NewObject(cls, ctor, params...);
      env->DeleteLocalRef(cls);
      return result;
    }

    // jobject is defined as _jobject*
    using SharedGlobalRef = std::shared_ptr<_jobject>;

    inline SharedGlobalRef makeSharedGlobalRef(JNIEnv *env, jobject localRef)
    {
      jobject globalRef = env->NewGlobalRef(localRef);
      return { globalRef, [](jobject globalRef) {
        // delegate the deletion to JNI
        qi::jni::JNIAttach attach;
        JNIEnv *env = attach.get();
        env->DeleteGlobalRef(globalRef);
      }};
    }
  }// !jni
}// !qi


JavaVM*       JVM(JNIEnv* env = 0);

// Signature conversion
std::string   toJavaSignature(const std::string &signature);
std::string   propertyBaseSignature(JNIEnv *env, jclass propertyBase);

jthrowable createNewException(JNIEnv *env, const char *className, const char *message, jthrowable cause);
jthrowable createNewException(JNIEnv *env, const char *className, const char *message);
jthrowable createNewException(JNIEnv *env, const char *className, jthrowable cause);
jthrowable createNewQiException(JNIEnv *env, const char *message);

// Java exception thrower
jint throwNew(JNIEnv *env, const char *className, const char *message = "");
jint throwNewException(JNIEnv *env, const char *message = "");
jint throwNewRuntimeException(JNIEnv *env, const char *message = "");
jint throwNewNullPointerException(JNIEnv *env, const char *message = "");

jint throwNewCancellationException(JNIEnv *env, const char *message = "");
jint throwNewExecutionException(JNIEnv *env, jthrowable cause);
jint throwNewTimeoutException(JNIEnv *env, const char *message = "");

jint throwNewDynamicCallException(JNIEnv *env, const char *message = "");
jint throwNewAdvertisementException(JNIEnv *env, const char *message = "");

#endif // !_JAVA_JNI_JNITOOLS_HPP_
