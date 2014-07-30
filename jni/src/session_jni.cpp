/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
*/

#include <stdexcept>

#include <qi/log.hpp>
#include <qi/api.hpp>
#include <qi/anyfunction.hpp>
#include <qi/session.hpp>

#include <jni.h>
#include <jnitools.hpp>
#include <session_jni.hpp>
#include <object_jni.hpp>
#include <callbridge.hpp>

qiLogCategory("qimessaging.jni");

jlong Java_com_aldebaran_qimessaging_Session_qiSessionCreate()
{
  qi::Session *session = new qi::Session();

  return (jlong) session;
}

void Java_com_aldebaran_qimessaging_Session_qiSessionDestroy(JNIEnv* QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pSession)
{
  qi::Session *s = reinterpret_cast<qi::Session*>(pSession);
  delete s;
}

jboolean Java_com_aldebaran_qimessaging_Session_qiSessionIsConnected(JNIEnv* QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pSession)
{
  qi::Session *s = reinterpret_cast<qi::Session*>(pSession);

  return (jboolean) s->isConnected();
}

static void adaptFuture(qi::Future<void> f, qi::Promise<qi::AnyValue> p)
{
  if (f.hasError())
    p.setError(f.error());
  else
    p.setValue(qi::AnyValue(qi::typeOf<void>()));
}

jlong Java_com_aldebaran_qimessaging_Session_qiSessionConnect(JNIEnv *env, jobject QI_UNUSED(obj), jlong pSession, jstring jurl)
{
  if (pSession == 0)
  {
    throwJavaError(env, "Given qi::Session doesn't exists (pointer null).");
    return 0;
  }

  // After this function return, callbacks are going to be set on new created Future.
  // Save the JVM pointer here to avoid big issues when callbacks will be called.
  JVM(env);
  qi::Future<qi::AnyValue> *fref = new qi::Future<qi::AnyValue>();
  qi::Promise<qi::AnyValue> promise;
  *fref = promise.future();
  qi::Session *s = reinterpret_cast<qi::Session*>(pSession);
  try
  {
    qi::Future<void> f = s->connect(qi::jni::toString(jurl));
    f.connect(adaptFuture, _1, promise);
  }
  catch (const std::exception& e)
  {
    promise.setError(e.what());
  }
  return (jlong) fref;
}

void Java_com_aldebaran_qimessaging_Session_qiSessionClose(JNIEnv* QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pSession)
{
  qi::Session *s = reinterpret_cast<qi::Session*>(pSession);

  s->close();
}

jobject   Java_com_aldebaran_qimessaging_Session_service(JNIEnv* env, jobject QI_UNUSED(obj), jlong pSession, jstring jname)
{
  qi::Session *s = reinterpret_cast<qi::Session*>(pSession);
  std::string serviceName = qi::jni::toString(jname);

  qi::AnyObject *obj = new qi::AnyObject();
  jobject proxy = 0;

  try
  {
    *obj = s->service(serviceName);
    JNIObject jniProxy(obj);
    return jniProxy.object();
  }
  catch (std::runtime_error &e)
  {
    delete obj;
    throwJavaError(env, e.what());
    return proxy;
  }
}

jint  Java_com_aldebaran_qimessaging_Session_registerService(JNIEnv *env, jobject QI_UNUSED(jobj), jlong pSession, jstring jname, jobject object)
{
  qi::Session*    session = reinterpret_cast<qi::Session*>(pSession);
  std::string     name    = qi::jni::toString(jname);
  JNIObject obj(object);
  jint ret = 0;

  try
  {
    ret = session->registerService(name, obj.objectPtr());
  }
  catch (std::runtime_error &e)
  {
    qiLogError() << "Throwing exception : " << e.what();
    throwJavaError(env, e.what());
    return 0;
  }

  if (ret <= 0)
  {
    throwJavaError(env, "Cannot register service");
    return 0;
  }

  return ret;
}

void  Java_com_aldebaran_qimessaging_Session_unregisterService(JNIEnv *env, jobject obj, jlong pSession, jint serviceId)
{
  qi::Session*    session = reinterpret_cast<qi::Session*>(pSession);
  unsigned int    id = static_cast<unsigned int>(serviceId);

  session->unregisterService(id);
}

void      Java_com_aldebaran_qimessaging_Session_onDisconnected(JNIEnv *env, jobject jobj, jlong pSession, jstring jcallbackName, jobject jobjectInstance)
{
  extern MethodInfoHandler gInfoHandler;
  qi::Session*    session = reinterpret_cast<qi::Session*>(pSession);
  std::string     callbackName = qi::jni::toString(jcallbackName);
  std::string     signature;
  qi_method_info*            data;

  // Create a new global reference on object instance.
  // jobject structure are local reference and are destroyed when returning to JVM
  jobjectInstance = env->NewGlobalRef(jobjectInstance);

  // Create a struct holding a jobject instance, jmethodId id and other needed thing for callback
  // Pass it to void * data to register_method
  signature = callbackName + "::(s)";
  data = new qi_method_info(jobjectInstance, signature, jobj);
  gInfoHandler.push(data);

  session->disconnected.connect(
      qi::AnyFunction::fromDynamicFunction(
          boost::bind(&event_callback_to_java, (void*) data, _1))).
          setCallType(qi::MetaCallType_Direct);
}
