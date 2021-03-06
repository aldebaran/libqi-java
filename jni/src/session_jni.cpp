/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#include <stdexcept>

#include <qi/log.hpp>
#include <qi/api.hpp>
#include <qi/anyfunction.hpp>
#include <qi/session.hpp>

#include <jni.h>
#include <jni/jnitools.hpp>
#include <jni/session_jni.hpp>
#include <jni/object_jni.hpp>
#include <jni/callbridge.hpp>
#include <jni/jobjectconverter.hpp>
#include <jni/future_jni.hpp>

#include <qi/messaging/clientauthenticator.hpp>
#include <qi/messaging/clientauthenticatorfactory.hpp>

qiLogCategory("qimessaging.jni");

JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Session_qiSessionCreate(JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj))
{
  qi::Session *session = new qi::Session();

  return (jlong) session;
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_Session_qiSessionDestroy(JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pSession)
{
  qi::Session *s = reinterpret_cast<qi::Session*>(pSession);
  delete s;
}

JNIEXPORT jboolean JNICALL Java_com_aldebaran_qi_Session_qiSessionIsConnected(JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pSession)
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

JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Session_qiSessionConnect(JNIEnv *env, jobject QI_UNUSED(obj), jlong pSession, jstring jurl)
{
  if (pSession == 0)
  {
    throwNewException(env, "Given qi::Session doesn't exists (pointer null).");
    return 0;
  }

  // After this function return, callbacks are going to be set on new created Future.
  // Save the JVM pointer here to avoid big issues when callbacks will be called.
  qi::Promise<qi::AnyValue> promise;
  auto fut = toJavaFuture(env, promise.future());

  qi::Session *s = reinterpret_cast<qi::Session*>(pSession);
  try
  {
    qi::Future<void> f = s->connect(qi::jni::toString(jurl));
    f.connect(boost::bind(adaptFuture, _1, promise));
  }
  catch (const std::exception& e)
  {
    promise.setError(e.what());
  }
  return env->NewLocalRef(fut.value);
}

JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Session_qiSessionClose(JNIEnv *env, jobject QI_UNUSED(obj), jlong pSession)
{
  qi::Session *session = reinterpret_cast<qi::Session*>(pSession);
  qi::Future<void> closeFuture = session->close();
  auto fut = toJavaFuture(env, closeFuture);
  return env->NewLocalRef(fut.value);
}

JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Session_service(JNIEnv *env, jobject QI_UNUSED(obj), jlong pSession, jstring jname)
{
  qi::Session *s = reinterpret_cast<qi::Session*>(pSession);
  std::string serviceName = qi::jni::toString(jname);

  try
  {
    qi::Future<qi::AnyObject> serviceFuture = s->service(serviceName);
    // qiFutureCallGet() requires that the future returns a qi::AnyValue
    qi::Future<qi::AnyValue> future = qi::toAnyValueFuture(std::move(serviceFuture));
    auto fut = toJavaFuture(env, future);
    return env->NewLocalRef(fut.value);
  }
  catch (std::runtime_error &e)
  {
    throwNewRuntimeException(env, e.what());
    return 0;
  }
}

JNIEXPORT jint JNICALL Java_com_aldebaran_qi_Session_registerService(JNIEnv *env, jobject QI_UNUSED(obj), jlong pSession, jstring jname, jobject object)
{
  qi::Session*    session = reinterpret_cast<qi::Session*>(pSession);
  std::string     name    = qi::jni::toString(jname);
  jint ret = 0;

  try
  {
    auto anyObj = qi::jni::internalQiAnyObject(object, *env);
    ret = session->registerService(name, std::move(anyObj)).value();
  }
  catch (std::runtime_error &e)
  {
    qiLogError() << "Throwing exception : " << e.what();
    throwNewException(env, e.what());
    return 0;
  }

  if (ret <= 0)
  {
    throwNewException(env, "Cannot register service");
    return 0;
  }

  return ret;
}

JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Session_unregisterService(JNIEnv *env, jobject QI_UNUSED(obj), jlong pSession, jint serviceId)
{
  qi::Session*    session = reinterpret_cast<qi::Session*>(pSession);
  unsigned int    id = static_cast<unsigned int>(serviceId);
  qi::Future<void> unregisterFuture = session->unregisterService(id);
  auto fut = toJavaFuture(env, unregisterFuture);
  return env->NewLocalRef(fut.value);
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_Session_onDisconnected(JNIEnv *env, jobject jobj, jlong pSession, jstring jcallbackName, jobject jobjectInstance)
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
  // FIXME jobj is not a global ref, it may be invalid when it will be used
  data = new qi_method_info(jobjectInstance, signature, jobj);
  gInfoHandler.push(data);

  session->disconnected.connect(
      qi::AnyFunction::fromDynamicFunction(
          boost::bind(&event_callback_to_java, (void*) data, _1)));
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_Session_addConnectionListener(JNIEnv *env, jobject QI_UNUSED(obj), jlong pSession, jobject listener)
{
  qi::Session *session = reinterpret_cast<qi::Session*>(pSession);
  auto gListener = qi::jni::makeSharedGlobalRef(env, listener);
  session->connected.connect([gListener] {
    qi::jni::JNIAttach attach;
    JNIEnv *env = attach.get();
    qi::jni::Call<void>::invoke(env, gListener.get(), "onConnected", "()V");
  });
  session->disconnected.connect([gListener](const std::string &reason) {
    qi::jni::JNIAttach attach;
    JNIEnv *env = attach.get();
    qi::jni::Call<void>::invoke(env, gListener.get(), "onDisconnected", "(Ljava/lang/String;)V", qi::jni::toJstring(reason));
  });
}

class Java_ClientAuthenticator : public qi::ClientAuthenticator
{
public:
  Java_ClientAuthenticator(JNIEnv* env, jobject object)
  {
    _jobjectRef = qi::jni::makeSharedGlobalRef(env, object);
  }

  qi::CapabilityMap initialAuthData() override
  {
    qi::jni::JNIAttach envAttach;
    const auto env = envAttach.get();
    const auto obj = _jobjectRef.get();
    const auto ca =
      ka::scoped(qi::jni::Call<jobject>::invoke(env, obj, "initialAuthData", "()Ljava/util/Map;"),
                 &qi::jni::releaseObject);
    return qi::jni::toCppStringAnyValueMap(*env, ca.value);
  }

  qi::CapabilityMap _processAuth(const qi::CapabilityMap &authData) override
  {
    qi::jni::JNIAttach envAttach;
    const auto env = envAttach.get();
    const auto obj = _jobjectRef.get();
    const auto jmap =
      ka::scoped(qi::jni::toJavaStringObjectMap(*env, authData), &qi::jni::releaseObject);
    const auto ca =
      ka::scoped(qi::jni::Call<jobject>::invoke(env, obj, "_processAuth",
                                                "(Ljava/util/Map;)Ljava/util/Map;", jmap.value),
                 &qi::jni::releaseObject);
    return qi::jni::toCppStringAnyValueMap(*env, ca.value);
  }

private:
  qi::jni::SharedGlobalRef _jobjectRef;
};

class Java_ClientAuthenticatorFactory : public qi::ClientAuthenticatorFactory
{
public:
  Java_ClientAuthenticatorFactory(JNIEnv* env, jobject object)
  {
    _jobject = env->NewGlobalRef(object);
  }

  qi::ClientAuthenticatorPtr newAuthenticator() override
  {
   JNIEnv* env = nullptr;
#ifndef ANDROID
    javaVirtualMachine->AttachCurrentThread(reinterpret_cast<void**>(&env), nullptr);
#else
    javaVirtualMachine->AttachCurrentThread(&env, nullptr);
#endif

    jobject ca = qi::jni::Call<jobject>::invoke(env, _jobject, "newAuthenticator", "()Lcom/aldebaran/qi/ClientAuthenticator;");
    auto result = boost::make_shared<Java_ClientAuthenticator>(env, ca);
    env->DeleteLocalRef(ca);
    javaVirtualMachine->DetachCurrentThread();
    return result;
  }

private:
  jobject _jobject;
};

JNIEXPORT void JNICALL Java_com_aldebaran_qi_Session_setClientAuthenticatorFactory(JNIEnv* env, jobject QI_UNUSED(obj), jlong pSession, jobject object)
{
  qi::Session* session = reinterpret_cast<qi::Session*>(pSession);
  session->setClientAuthenticatorFactory(boost::make_shared<Java_ClientAuthenticatorFactory>(env, object));
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_Session_loadService(JNIEnv *env, jobject obj, jlong pSession, jstring jname)
{
  qi::Session* session = reinterpret_cast<qi::Session*>(pSession);
  std::string moduleName = qi::jni::toString(jname);
  session->loadService(moduleName);
  return;
}

/**
 * @brief List of URL session end points.
 * @param env JNI environment.
 * @param obj Object Java instance.
 * @param pSession Reference to session in JNI.
 * @param endpointsList List to add URL session endpoints. (List<String>)
 */
JNIEXPORT void JNICALL Java_com_aldebaran_qi_Session_endpoints(JNIEnv * env, jobject obj, jlong pSession, jobject endpointsList)
{
  qi::Session* session = reinterpret_cast<qi::Session*>(pSession);
  std::vector<qi::Url> endpoints = session->endpoints();
  jmethodID methodAdd = env->GetMethodID(cls_list, "add", "(Ljava/lang/Object;)Z");

  for (std::vector<qi::Url>::iterator it = endpoints.begin(); it != endpoints.end(); ++it)
  {
    jstring url = qi::jni::toJstring((*it).str());
    env->CallBooleanMethod(endpointsList, methodAdd, url);
    qi::jni::releaseString(url);
  }
}

/**
 * @brief Wait for a service is connected
 * @param env JNI environment.
 * @param obj Object Java instance.
 * @param pointerSession Pointer on current session
 * @param jServiceName Service name to wait its connection
 * @return Future java object to track/link the connection
 */
JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Session_waitForService(JNIEnv * env, jobject obj, jlong pointerSession,  jstring jServiceName)
{
  qi::Session* session = reinterpret_cast<qi::Session*>(pointerSession);
  std::string serviceName = qi::jni::toString(jServiceName);
  qi::Future<void> waitForServiceFuture = session->waitForService(serviceName);
  auto fut = toJavaFuture(env, waitForServiceFuture);
  return env->NewLocalRef(fut.value);
}
