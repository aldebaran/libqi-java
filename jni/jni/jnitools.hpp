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

#include <qi/property.hpp>

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

extern jclass cls_void;
extern jclass cls_string;
extern jclass cls_integer;
extern jclass cls_float;
extern jclass cls_double;
extern jclass cls_long;
extern jclass cls_boolean;

extern jclass cls_future;
extern jfieldID field_future_pointer;
extern jclass cls_anyobject;
extern jclass cls_tuple;

extern jclass cls_list;
extern jclass cls_arraylist;

extern jclass cls_map;
extern jclass cls_hashmap;
extern jclass cls_enum;

extern jclass cls_object;
extern jclass cls_nativeTools;
extern jclass cls_throwable;
extern jmethodID method_NativeTools_callJava;
extern JavaVM* javaVirtualMachine;

/**
 * @brief Reference of Java class to report log
 */
extern jclass LogReportClass;
/**
 * @brief Method to call for report captured logs
 */
extern jmethodID jniLog;

// JNI utils
extern "C"
{
  JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* unused);
  JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* unused);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_EmbeddedTools_initTypeSystem(JNIEnv* env,
                                                                            jclass unused = nullptr);
} // !extern C

/**
 * @brief Log handler that capture logs and send them to Java side
 */
class JNIlogHandler
{
public:
    JNIlogHandler();
    ~JNIlogHandler();

    /**
     * \brief Prints a log message on the console.
     * \param verb verbosity of the log message.
     * \param date qi::Clock date at which the log message was issued.
     * \param date qi::SystemClock date at which the log message was issued.
     * \param category will be used in future for filtering
     * \param msg actual message to log.
     * \param file filename from which this log message was issued.
     * \param fct function name from which this log message was issued.
     * \param line line number in the issuer file.
     */
    void log(const qi::LogLevel verb,
             const qi::Clock::time_point date,
             const qi::SystemClock::time_point systemDate,
             const char* category,
             const char* msg,
             const char* file,
             const char* fct,
             const int line);
};

/**
 * @brief Instance of registered JNI log capture handler.
 * Global to maintain it alive durring all the application life
 */
extern JNIlogHandler* jniLogHandler;

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
    boost::optional<std::string> name(jclass clazz);
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
                      Types&&...    rest)
      {
        const auto clazz = ka::scoped(env->GetObjectClass(jobject), qi::jni::releaseClazz);
        return invoke(env, clazz.value, jobject, methodName, methodSig,
                      std::forward<Types>(rest)...);
      }

      template <typename... Types>
      static R invoke(JNIEnv*     env,
                      jclass      clazz,
                      jobject     jobject,
                      const char* methodName,
                      const char* methodSig,
                      Types&&...    rest)
      {
        const auto mid = env->GetMethodID(clazz, methodName, methodSig);
        return invokeImpl(env, jobject, mid, std::forward<Types>(rest)...);
      }

    private:
      template <typename... Types>
      static R invokeImpl(JNIEnv* env,
                          jobject jobject,
                          jmethodID methodId,
                          Types&&... rest);
    };

    template <>
    template <typename... Types>
    void Call<void>::invokeImpl(JNIEnv* env, jobject jobject, jmethodID mid, Types&&... rest)
    {
      return env->CallVoidMethod(jobject, mid, std::forward<Types>(rest)...);
    }

    template <>
    template <typename... Types>
    jboolean Call<jboolean>::invokeImpl(JNIEnv* env,
                                        jobject jobject,
                                        jmethodID mid,
                                        Types&&... rest)
    {
      return env->CallBooleanMethod(jobject, mid, std::forward<Types>(rest)...);
    }

    template <>
    template <typename... Types>
    jobject Call<jobject>::invokeImpl(JNIEnv* env, jobject jobject, jmethodID mid, Types&&... rest)
    {
      return env->CallObjectMethod(jobject, mid, std::forward<Types>(rest)...);
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

    /// If a Java exception is pending, fetches it, clears its pending status and then throws a
    /// std::runtime_error with the same error message. If no exception is pending,
    /// returns immediately.
    void handlePendingException(JNIEnv& env);

    /// Returns a string describing the given JNI error code.
    const char* errorToString(jint code);

    /// Raises a java.lang.AssertionError with the given message if the condition is false and
    /// return the value of the condition.
    bool assertion(JNIEnv* env, bool condition, const char* message = "");

    /// Raises a java.lang.NullPointerException with the given message and return true if the object
    /// is null.
    bool throwIfNull(JNIEnv* env, jobject obj, const char* message = "Object is null.");
    bool throwIfNull(JNIEnv* env, jlong ptr, const char* message = "Pointer is null.");
  }// !jni
}// !qi

// Signature conversion
std::string   toJavaSignature(const std::string &signature);
std::string toJavaSignatureWithFuture(const std::string &signature);

/**
 * @brief Gets the qitype signature of a Java class.
 */
std::string propertyBaseSignature(JNIEnv *env, jclass propertyBase);

/**
 * @brief Returns the corresponding type interface of a Property value Java class.
 */
qi::TypeInterface* propertyValueClassToType(JNIEnv *env, jclass clazz);

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
jint throwNewApplicationException(JNIEnv *env, const char *message = "");
jint throwNewConnectionException(JNIEnv *env, const char *message = "");
jint throwNewPostException(JNIEnv *env, const char *message = "");
jint throwNewSessionException(JNIEnv *env, const char *message = "");
jint throwNewIllegalStateException(JNIEnv *env, const char *message = "");

/**
 * Manages a property and maintains a global reference of the Java value to keep it alive
 * during the lifetime of the property.
 */
class PropertyManager
{
public:
  /**Last setted global reference*/
  jobject globalReference;

  /**The managed property*/
  std::unique_ptr<qi::GenericProperty> property;

  /**
   * @brief Create the manager with a property with an unspecified default value.
   */
  explicit PropertyManager(qi::TypeInterface &valueType)
    : globalReference{ nullptr }
    , property{ new qi::GenericProperty{ &valueType } }
  {
  }

  /**
   * @brief Create the manager with a property with a given value.
   *
   * Passing a value that refers to a null Java object is undefined behavior.
   * TODO: Make it defined behavior by handling it in the conversion between AnyValue and jobject.
   */
  PropertyManager(qi::TypeInterface& valueType, JNIEnv& env, jobject value)
    : globalReference{ env.NewGlobalRef(value) }
    , property{ new qi::GenericProperty{
        qi::AnyValue::from(globalReference).convertCopy(&valueType) } }
  {
  }

  /**
   * @brief Clear global reference if need
   * @param env JNI environment
   */
  void clearReference(JNIEnv *env)
  {
    if(env->IsSameObject(globalReference, nullptr) == JNI_FALSE)
    {
      env->DeleteGlobalRef(globalReference);
      globalReference = nullptr;
    }
  }

  /**
   * @brief Destroy properly the manager
   * @param env JNI environment
   */
  void destroy(JNIEnv *env)
  {
    property.reset();
    clearReference(env);
  }

  /**
   * @brief Change the value, store it properly to keep alive the reference
   * @param env JNI evironment
   * @param value New value
   */
  void setValue(JNIEnv *env, jobject value)
  {
    clearReference(env);

    if(env->IsSameObject(value, nullptr) == JNI_TRUE)
    {
      globalReference = nullptr;
    }
    else
    {
      globalReference = env->NewGlobalRef(value);
    }

    QI_ASSERT_NOT_NULL(property);
    property->setValue(qi::AnyValue::from(globalReference)).wait();
  }
};


#endif // !_JAVA_JNI_JNITOOLS_HPP_
