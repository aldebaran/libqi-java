/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#include <qi/log.hpp>
#include <sys/types.h>
#include <signal.h>
#include <qi/signature.hpp>
#include <qi/session.hpp>
#include "jnitools.hpp"

#include <boost/thread/tss.hpp>

qiLogCategory("qimessaging.jni");

jclass cls_void;
jclass cls_string;
jclass cls_integer;
jclass cls_float;
jclass cls_double;
jclass cls_long;
jclass cls_boolean;

jclass cls_future;
jfieldID field_future_pointer;
jclass cls_anyobject;
jclass cls_tuple;

jclass cls_list;
jclass cls_arraylist;

jclass cls_map;
jclass cls_hashmap;
jclass cls_enum;
jclass cls_object;
jclass cls_nativeTools;
jmethodID method_NativeTools_callJava;

/**
 * @brief Reference of Java class to report log
 */
jclass LogReportClass;
/**
 * @brief Method to call for report captured logs
 */
jmethodID jniLog;

JavaVM* javaVirtualMachine = nullptr;

/**
 * @brief Instance of registered JNI log capture handler.
 * Global to maintain it alive durring all the application life
 */
JNIlogHandler* jniLogHandler = nullptr;

static void emergency()
{
  qiLogFatal() << "Emergency, aborting";
#ifdef unix
  kill(getpid(), SIGQUIT);
#else
  abort();
#endif
}

JNIlogHandler::JNIlogHandler()
{
}

JNIlogHandler::~JNIlogHandler()
{
}

/**
 * \brief Capture a log message and send it to Java side
 * \param verb verbosity of the log message.
 * \param date qi::Clock date at which the log message was issued.
 * \param date qi::SystemClock date at which the log message was issued.
 * \param category will be used in future for filtering
 * \param msg actual message to log.
 * \param file filename from which this log message was issued.
 * \param fct function name from which this log message was issued.
 * \param line line number in the issuer file.
 */
void JNIlogHandler::log(const qi::LogLevel verb,
                        const qi::Clock::time_point date,
                        const qi::SystemClock::time_point systemDate,
                        const char* category,
                        const char* msg,
                        const char* file,
                        const char* fct,
                        const int line)
{
    if(!msg)
    {
        //No message, nothing to log
       return;
    }

    jint logLevel = (jint)verb;
    JNIEnv* env = nullptr;
    jint succeed = -1;
    bool attached = false;

    //Get environement directly. Succeed if called from JNI method
    succeed = javaVirtualMachine->GetEnv((void**)&env, JNI_VERSION_1_6);

    if(succeed != JNI_OK || !env)
    {
        //The environment is not get, it is maybe because we are not inside a JNI thread
        //So we try to attach JNI environment to current thread. Have to detach it in success
        char threadName[] = "qimessaging-reportLog-thread";
        JavaVMAttachArgs args = { JNI_VERSION_1_6, threadName, nullptr };
        succeed = javaVirtualMachine->AttachCurrentThread((envPtr)&env, &args);

        if(succeed != JNI_OK || !env)
        {
            //Issue to get the environemnt, can't continue
            return;
        }

        //Attach succeed, we remeber it is attached to detach it later
        attached = true;
    }


    //Send the message to Java side, if available
    if (LogReportClass)
    {
      try
      {
          jstring message = env->NewStringUTF(msg);
          env->CallStaticVoidMethod(LogReportClass, jniLog, logLevel, message);
      }
      catch (...)
      {
          // Exception while report log
      }
    }


    if(attached)
    {
        //Detach from current thread if need
        javaVirtualMachine->DetachCurrentThread();
    }
}

JNIEXPORT jint JNICALL JNI_OnLoad (JavaVM* virtualMachine, void* QI_UNUSED(reserved))
{
  javaVirtualMachine = virtualMachine;
  // seems like a good number
  qi::getEventLoop()->setMaxThreads(8);
  qi::getEventLoop()->setEmergencyCallback(emergency);
  return QI_JNI_MIN_VERSION;
}

static inline jclass loadClass(JNIEnv *env, const char *className)
{
  return reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass(className)));
}

static void init_classes(JNIEnv *env)
{
  cls_void = loadClass(env, "java/lang/Void");
  cls_string = loadClass(env, "java/lang/String");
  cls_integer = loadClass(env, "java/lang/Integer");
  cls_float = loadClass(env, "java/lang/Float");
  cls_double = loadClass(env, "java/lang/Double");
  cls_long = loadClass(env, "java/lang/Long");
  cls_boolean = loadClass(env, "java/lang/Boolean");

  cls_future = loadClass(env, "com/aldebaran/qi/Future");
  field_future_pointer = env->GetFieldID(cls_future, "_fut", "J");
  cls_anyobject = loadClass(env, "com/aldebaran/qi/AnyObject");
  cls_tuple = loadClass(env, "com/aldebaran/qi/Tuple");

  cls_list = loadClass(env, "java/util/List");
  cls_arraylist = loadClass(env, "java/util/ArrayList");

  cls_map = loadClass(env, "java/util/Map");
  cls_hashmap = loadClass(env, "java/util/HashMap");

  cls_object =loadClass(env, "java/lang/Object");
  cls_nativeTools = loadClass(env, "com/aldebaran/qi/NativeTools");
  cls_enum = loadClass(env, "java/lang/Enum");

  method_NativeTools_callJava = env->GetStaticMethodID(cls_nativeTools,
                                                       "callJava",
                                                       "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");

  // KLUDGE: LogReport has been written outside, and may not be available.
  LogReportClass = loadClass(env, "com/aldebaran/qi/log/LogReport");
  jniLog = LogReportClass ? env->GetStaticMethodID(LogReportClass, "jniLog", "(ILjava/lang/String;)V") : nullptr;
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_EmbeddedTools_initTypeSystem(JNIEnv* env, jclass QI_UNUSED(cls))
{
  init_classes(env);

  //Register log handler that capture logs
  jniLogHandler = new JNIlogHandler();
  qi::log::addHandler("jniLogHandler",
                      boost::bind(&JNIlogHandler::log,
                                  jniLogHandler,
                                  _1, _2, _3, _4, _5, _6, _7, _8),
                      qi::LogLevel_Debug);
}

/**
 * @brief getJavaSignature Convert qitype-like signature into Java-like signature.
 * @param sig Java signature to be.
 * @param sigInfo qitype signature.
 *
 * Do not call this function, use toJavaSignature instead
 * @see toJavaSignature
 */
void getJavaSignature(std::string &javaSignature, const std::string& qiSignature)
{
  unsigned int i = 0;

  while (i < qiSignature.size())
  {
    switch (qiSignature[i])
    {
    case qi::Signature::Type_Bool:
      javaSignature.append("Ljava/lang/Boolean;");
      break;
    case qi::Signature::Type_Int8:
      javaSignature.append("Ljava/lang/Character;");
      break;
    case qi::Signature::Type_Float:
      javaSignature.append("Ljava/lang/Float;");
      break;
    case qi::Signature::Type_Double:
      javaSignature.append("Ljava/lang/Double;");
      break;
    case qi::Signature::Type_Int32:
      javaSignature.append("Ljava/lang/Integer;");
      break;
    case qi::Signature::Type_Int64:
      javaSignature.append("Ljava/lang/Long;");
      break;
    case qi::Signature::Type_String:
      javaSignature.append("Ljava/lang/String;");
      break;
    case qi::Signature::Type_Void:
      javaSignature.append("V");
      break;
    case qi::Signature::Type_Dynamic:
      javaSignature.append("Ljava/lang/Object;");
      break;
    case qi::Signature::Type_Map:
    {
      javaSignature.append("Ljava/util/Map;");
      while (i < qiSignature.size() && qiSignature[i] != qi::Signature::Type_Map_End)
        i++;
      break;
    }
    case qi::Signature::Type_Tuple:
    {
      javaSignature.append("Lcom/aldebaran/qi/Tuple;");
      while (i < qiSignature.size() && qiSignature[i] != qi::Signature::Type_Tuple_End)
        i++;
      break;
    }
    case qi::Signature::Type_List:
    {
      javaSignature.append("Ljava/util/ArrayList;");
      while (i < qiSignature.size() && qiSignature[i] != qi::Signature::Type_List_End)
        i++;
      break;
    }
    case qi::Signature::Type_Object:
    {
      javaSignature.append("L");
      javaSignature.append(QI_OBJECT_CLASS);
      javaSignature.append(";");
      break;
    }

    default:
      qiLogFatal() << "Unknown conversion for [" << qiSignature[i] << "]";
      break;
    }

    i++;
  }
}

/**
 * @brief toJavaSignature Convert qitype-like signature into Java-like signature.
 * @param signature qitype signature
 * @return Java signature
 */
std::string   toJavaSignature(const std::string &signature)
{
  std::vector<std::string> sigInfo = qi::signatureSplit(signature);
  std::string              sig;

  // Compute every arguments
  sig.append("(");
  getJavaSignature(sig, sigInfo[2].substr(1, sigInfo[2].size()-2));
  sig.append(")");

  // Finally add return signature (or 'V' if empty)
  if (sigInfo[0] == "")
    sig.append("V");
  else
    getJavaSignature(sig, sigInfo[0]);

  return sig;
}

/**
 * @brief toJavaSignatureWithFuture Convert qitype-like signature into Java-like signature
 * returning a Future.
 * @param signature qitype signature
 * @return Java signature
 */
std::string toJavaSignatureWithFuture(const std::string &signature)
{
  std::vector<std::string> sigInfo = qi::signatureSplit(signature);
  std::string              sig;

  // Compute every argument
  sig.append("(");
  getJavaSignature(sig, sigInfo[2].substr(1, sigInfo[2].size()-2));
  sig.append(")");

  // no need to specify the Future's result type
  sig.append("Lcom/aldebaran/qi/Future;");
  return sig;
}

jthrowable createNewException(JNIEnv *env, const char *className, const char *message, jthrowable cause)
{
  jstring jMessage = qi::jni::toJstring(message);
  jobject ex = qi::jni::construct(env, className, "(Ljava/lang/String;Ljava/lang/Throwable)V", jMessage, cause);
  return reinterpret_cast<jthrowable>(ex);
}

jthrowable createNewException(JNIEnv *env, const char *className, const char *message)
{
  jstring jMessage = qi::jni::toJstring(message);
  jobject ex = qi::jni::construct(env, className, "(Ljava/lang/String;)V", jMessage);
  return reinterpret_cast<jthrowable>(ex);
}

jthrowable createNewException(JNIEnv *env, const char *className, jthrowable cause)
{
  jobject ex = qi::jni::construct(env, className, "(Ljava/lang/Throwable;)V", cause);
  if (!ex)
  qiLogFatal() << className << " noex";
  return reinterpret_cast<jthrowable>(ex);
}

jthrowable createNewQiException(JNIEnv *env, const char *message)
{
  return createNewException(env, "com/aldebaran/qi/QiException", message);
}

/**
 * @brief throwNewException Helper function to throw generic Java exception from C++
 * @param env JNI environment
 * @param message content of exception
 * @return 0 on success, a positive number otherwise
 */
jint throwNew(JNIEnv *env, const char *className, const char *message)
{
  jclass exClass = env->FindClass(className);
  if (!exClass)
  {
    qiLogFatal() << "Cannot find exception class: " << className;
    return 1;
  }

  return env->ThrowNew(exClass, message);
}

jint throwNewException(JNIEnv *env, const char *message)
{
  return throwNew(env, "java/lang/Exception", message);
}

jint throwNewRuntimeException(JNIEnv *env, const char *message)
{
  return throwNew(env, "java/lang/RuntimeException", message);
}

jint throwNewNullPointerException(JNIEnv *env, const char *message)
{
  return throwNew(env, "java/lang/NullPointerException", message);
}

jint throwNewCancellationException(JNIEnv *env, const char *message)
{
  return throwNew(env, "java/util/concurrent/CancellationException", message);
}

jint throwNewExecutionException(JNIEnv *env, jthrowable cause)
{
  jthrowable ex = createNewException(env, "java/util/concurrent/ExecutionException", cause);
  if (!ex)
    return 1;
  return env->Throw(ex);
}

jint throwNewTimeoutException(JNIEnv *env, const char *message)
{
  return throwNew(env, "java/util/concurrent/TimeoutException", message);
}

jint throwNewDynamicCallException(JNIEnv *env, const char *message)
{
  return throwNew(env, "com/aldebaran/qi/DynamicCallException", message);
}

jint throwNewAdvertisementException(JNIEnv *env, const char *message)
{
  return throwNew(env, "com/aldebaran/qi/AdvertisementException", message);
}

jint throwNewApplicationException(JNIEnv *env, const char *message)
{
  return throwNew(env, "com/aldebaran/qi/ApplicationException", message);
}

jint throwNewConnectionException(JNIEnv *env, const char *message)
{
  return throwNew(env, "com/aldebaran/qi/ConnectionException", message);
}

jint throwNewPostException(JNIEnv *env, const char *message)
{
  return throwNew(env, "com/aldebaran/qi/PostException", message);
}

jint throwNewSessionException(JNIEnv *env, const char *message)
{
  return throwNew(env, "com/aldebaran/qi/SessionException", message);
}

jint throwNewIllegalStateException(JNIEnv *env, const char *message)
{
  return throwNew(env, "java/lang/IllegalStateException", message);
}

std::string propertyBaseSignature(JNIEnv *env, jclass propertyBase)
{
  std::string sig;

  if (env->IsAssignableFrom(propertyBase, cls_string))
    sig = static_cast<char>(qi::Signature::Type_String);
  if (env->IsAssignableFrom(propertyBase, cls_integer) || env->IsAssignableFrom(propertyBase, cls_enum))
    sig = static_cast<char>(qi::Signature::Type_Int32);
  if (env->IsAssignableFrom(propertyBase, cls_float))
    sig = static_cast<char>(qi::Signature::Type_Float);
  if (env->IsAssignableFrom(propertyBase, cls_boolean))
    sig = static_cast<char>(qi::Signature::Type_Bool);
  if (env->IsAssignableFrom(propertyBase, cls_long))
    sig = static_cast<char>(qi::Signature::Type_Int64);
  if (env->IsAssignableFrom(propertyBase, cls_anyobject))
    sig = static_cast<char>(qi::Signature::Type_Object);
  if (env->IsAssignableFrom(propertyBase, cls_double))
    sig = static_cast<char>(qi::Signature::Type_Float);
  if (env->IsAssignableFrom(propertyBase, cls_map))
  {
    sig = static_cast<char>(qi::Signature::Type_Map);
    sig += static_cast<char>(qi::Signature::Type_Dynamic);
    sig += static_cast<char>(qi::Signature::Type_Dynamic);
    sig += static_cast<char>(qi::Signature::Type_Map_End);
  }
  if (env->IsAssignableFrom(propertyBase, cls_list))
  {
    sig = static_cast<char>(qi::Signature::Type_List);
    sig += static_cast<char>(qi::Signature::Type_Dynamic);
    sig += static_cast<char>(qi::Signature::Type_List_End);
  }
  if (env->IsAssignableFrom(propertyBase, cls_tuple))
  {
    sig = static_cast<char>(qi::Signature::Type_Tuple);
    sig += static_cast<char>(qi::Signature::Type_Dynamic);
    sig += static_cast<char>(qi::Signature::Type_Tuple_End);
  }

  return sig;
}

qi::TypeInterface *propertyValueClassToType(JNIEnv *env, jclass clazz)
{
  if (env->IsAssignableFrom(clazz, cls_string))
    return qi::typeOf<std::string>();
  if (env->IsAssignableFrom(clazz, cls_integer) || env->IsAssignableFrom(clazz, cls_enum))
    return qi::typeOf<std::int32_t>();
  if (env->IsAssignableFrom(clazz, cls_float))
    return qi::typeOf<float>();
  if (env->IsAssignableFrom(clazz, cls_boolean))
    return qi::typeOf<bool>();
  if (env->IsAssignableFrom(clazz, cls_long))
    return qi::typeOf<std::int64_t>();
  if (env->IsAssignableFrom(clazz, cls_anyobject))
    return qi::typeOf<qi::AnyObject>();
  if (env->IsAssignableFrom(clazz, cls_double))
    return qi::typeOf<double>();
  if (env->IsAssignableFrom(clazz, cls_map))
    return qi::typeOf<std::map<qi::AnyValue, qi::AnyValue>>();
  if (env->IsAssignableFrom(clazz, cls_list))
    return qi::typeOf<std::vector<qi::AnyValue>>();

  // For all other types, return a dynamic type. This includes the types that inherit the Tuple type
  // that we expose in Java because it is dynamic and the types it contains are unknown until we
  // instantiate one.
  return qi::typeOf<qi::AnyValue>();
}

namespace qi {
  namespace jni {

    namespace {
      struct JNIHandle
      {
        JNIHandle() :
          lockCount(0),
          env(0),
          attached(false)
        {}

        unsigned int lockCount;
        JNIEnv* env;
        bool attached;
      };
    }

    static boost::thread_specific_ptr<JNIHandle> ThreadJNI;

    JNIAttach::JNIAttach(JNIEnv* env)
    {
      if (!ThreadJNI.get())
        ThreadJNI.reset(new JNIHandle);
      if (env)
      {
        assert(!ThreadJNI->env || env == ThreadJNI->env);
        ThreadJNI->env = env;
      }
      else if (!ThreadJNI->env)
      {
        if (javaVirtualMachine->GetEnv((void**)&ThreadJNI->env, QI_JNI_MIN_VERSION) != JNI_OK ||
            ThreadJNI->env == 0)
        {
          char threadName[] = "qimessaging-thread";
          JavaVMAttachArgs args = { JNI_VERSION_1_6, threadName, 0 };
          if (javaVirtualMachine->AttachCurrentThread((envPtr)&ThreadJNI->env, &args) != JNI_OK ||
              ThreadJNI->env == 0)
          {
            qiLogError() << "Cannot attach callback thread to Java VM";
            throw std::runtime_error("Cannot attach callback thread to Java VM");
          }
          ThreadJNI->attached = true;
        }
      }
      ++ThreadJNI->lockCount;
    }

    JNIAttach::~JNIAttach()
    {
      assert(ThreadJNI->lockCount > 0);
      --ThreadJNI->lockCount;

      if (ThreadJNI->lockCount == 0)
      {
        if (ThreadJNI->attached)
        {
          if (javaVirtualMachine->DetachCurrentThread() != JNI_OK)
          {
            qiLogError() << "Cannot detach from current thread";
          }
          ThreadJNI->attached = false;
        }
        ThreadJNI->env = 0;
      }
    }

    JNIEnv* JNIAttach::get()
    {
      assert(ThreadJNI->lockCount > 0);
      assert(ThreadJNI->env);
      return ThreadJNI->env;
    }

    // Get JNI environment pointer, valid in current thread.
    JNIEnv*     env()
    {
      JNIEnv* env = 0;

      javaVirtualMachine->GetEnv(reinterpret_cast<void**>(&env), QI_JNI_MIN_VERSION);
      if (!env)
      {
        qiLogError() << "Cannot get JNI environment from JVM";
        return 0;
      }

      if (javaVirtualMachine->AttachCurrentThread(reinterpret_cast<envPtr>(&env), (void*)0) != JNI_OK)
      {
        qiLogError() << "Cannot attach current thread to JVM";
        return 0;
      }

      return env;
    }

    jclass      clazz(jobject object)
    {
      JNIEnv*   env = qi::jni::env();

      if (!env)
        return 0;

      return (jclass) env->GetObjectClass(object);
    }

    // Release local ref to avoid JNI internal reference table overflow
    void        releaseClazz(jclass clazz)
    {
      JNIEnv*   env = qi::jni::env();

      if (!env || !clazz)
      {
        qiLogError() << "Cannot release local class reference. (Env: " << env << ", Clazz: " << clazz << ")";
        return;
      }

      env->DeleteLocalRef(clazz);
    }

    // Convert jstring into std::string
    // Use of std::string ensures ref leak safety.
    std::string toString(jstring inputString)
    {
      std::string string;
      const char *cstring = 0;
      JNIEnv*   env = qi::jni::env();

      if (!env)
        return string;

      if (!(cstring = env->GetStringUTFChars(inputString, 0)))
      {
        qiLogError() << "Cannot convert Java string into string.";
        return string;
      }

      string = cstring;
      env->ReleaseStringUTFChars(inputString, cstring);
      return string;
    }

    // Convert std::string into jstring
    // Use qi::jni::releaseString to avoir ref leak.
    jstring     toJstring(const std::string& input)
    {
      jstring   string = 0;
      JNIEnv*   env = qi::jni::env();

      if (!env)
        return string;

      if (!(string = env->NewStringUTF(input.c_str())))
        qiLogError() << "Cannot convert string into Java string.";

      return string;
    }

    // Remove local ref on jstring created with qi::jni::toJstring
    void        releaseString(jstring input)
    {
      JNIEnv*   env = qi::jni::env();

      if (!env)
        return;

      env->DeleteLocalRef(input);
    }

    // Release local ref on JNI object
    void        releaseObject(jobject obj)
    {
      JNIEnv*   env = qi::jni::env();

      if (!env)
        return;

      env->DeleteLocalRef(obj);
    }

    jobjectArray toJobjectArray(const std::vector<AnyReference> &values)
    {
      JNIEnv *env = qi::jni::env();
      if (!env)
        return nullptr;

      jclass objectClass = env->FindClass("java/lang/Object");
      jobjectArray array = env->NewObjectArray(values.size(), objectClass, nullptr);
      qi::jni::releaseClazz(objectClass);
      int i = 0;
      for (const AnyReference &ref : values)
      {
        // FIXME:
        // We cannot use `qi::AnyReference::to<jobject>()` because it would return a jobject that
        // was already released:
        // The `to` method converts the AnyReference into a temporary AnyReference to jobject,
        // copies it in a local jobject (which is a pointer), destroys the temporary (which calls
        // `JNIEnv::DestroyGlobalRef`) and returns the copy of the jobject, which then references a
        // object that was released. Instead we call `AnyReference::convert` directly a use the
        // jobject before the result of the `convert` method is destroyed.
        const auto converted = ref.convert(qi::typeOf<jobject>());
        QI_ASSERT(converted->isValid());
        const auto value = *reinterpret_cast<jobject*>(converted->rawValue());
        env->SetObjectArrayElement(array, i++, value);
      }
      return array;
    }
  }// !jni
}// !qi
