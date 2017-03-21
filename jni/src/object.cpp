/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#include <qi/anyobject.hpp>
#include <qi/jsoncodec.hpp>

#include <jnitools.hpp>
#include <object.hpp>
#include <callbridge.hpp>
#include <jobjectconverter.hpp>

qiLogCategory("qimessaging.jni");

extern MethodInfoHandler gInfoHandler;

JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_AnyObject_property(JNIEnv* env, jobject QI_UNUSED(jobj), jlong pObj, jstring name)
{
  qi::AnyObject&     obj = *(reinterpret_cast<qi::AnyObject*>(pObj));
  std::string        propName = qi::jni::toString(name);

  qi::Future<qi::AnyValue>* ret = new qi::Future<qi::AnyValue>();

  qi::jni::JNIAttach attach(env);

  try
  {
    *ret = obj.property<qi::AnyValue>(propName);
  } catch (qi::FutureUserException& e)
  {
    delete ret;
    throwNewDynamicCallException(env, e.what());
    return 0;
  }

  return (jlong) ret;
}

JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_AnyObject_setProperty(
    JNIEnv* env, jobject /*jObject*/, jlong objectAddress, jstring jPropertyName, jobject jValue)
{
  auto objectPtr = reinterpret_cast<qi::AnyObject*>(objectAddress);
  auto propertyName = qi::jni::toString(jPropertyName);

  qi::jni::JNIAttach attach(env);
  try
  {
    std::unique_ptr<qi::Future<qi::AnyValue>> futurePtr{new qi::Future<qi::AnyValue>()};
    *futurePtr = qi::toAnyValueFuture(objectPtr->setProperty(propertyName, qi::AnyValue::from<jobject>(jValue)).async());
    return reinterpret_cast<jlong>(futurePtr.release());
  }
  catch (std::runtime_error &e)
  {
    throwNewDynamicCallException(env, e.what());
    return 0;
  }
}

JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_AnyObject_asyncCall(JNIEnv* env, jobject QI_UNUSED(jobj), jlong pObject, jstring jmethod, jobjectArray args)
{
  qi::AnyObject&    obj = *(reinterpret_cast<qi::AnyObject*>(pObject));
  std::string       method;
  qi::Future<qi::AnyValue>* fut = 0;

  qi::jni::JNIAttach attach(env);

  // Get method name and parameters C style.
  method = qi::jni::toString(jmethod);
  try {
    fut = call_from_java(env, obj, method, args);
  } catch (std::exception& e)
  {
    throwNewDynamicCallException(env, e.what());
    return 0;
  }
  return (jlong) fut;
}

JNIEXPORT jstring JNICALL Java_com_aldebaran_qi_AnyObject_printMetaObject(JNIEnv* QI_UNUSED(env), jobject QI_UNUSED(jobj), jlong pObject)
{
  qi::AnyObject&    obj = *(reinterpret_cast<qi::AnyObject*>(pObject));
  std::stringstream ss;

  qi::detail::printMetaObject(ss, obj.metaObject());
  return qi::jni::toJstring(ss.str());
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_AnyObject_destroy(JNIEnv* QI_UNUSED(env), jobject QI_UNUSED(jobj), jlong pObject)
{
  qi::AnyObject*    obj = reinterpret_cast<qi::AnyObject*>(pObject);

  delete obj;
}


JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_AnyObject_disconnect(JNIEnv *env, jobject QI_UNUSED(jobj), jlong pObject, jlong subscriberId)
{
  qi::AnyObject&             obj = *(reinterpret_cast<qi::AnyObject *>(pObject));
  try {
    obj.disconnect(subscriberId);
  } catch (std::exception& e)
  {
    throwNewRuntimeException(env, e.what());
  }
  return 0;
}

JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_AnyObject_connect(JNIEnv *env, jobject jobj, jlong pObject, jstring method, jobject instance, jstring service, jstring eventName)
{
  qi::AnyObject&             obj = *(reinterpret_cast<qi::AnyObject *>(pObject));
  std::string                signature = qi::jni::toString(method);
  std::string                event = qi::jni::toString(eventName);
  qi_method_info*            data;
  std::vector<std::string>  sigInfo;

  // Keep a pointer on JavaVM singleton if not already set.
  JVM(env);

  // Create a new global reference on object instance.
  // jobject structure are local reference and are destroyed when returning to JVM
  instance = env->NewGlobalRef(instance);

  // Remove return value
  sigInfo = qi::signatureSplit(signature);
  signature = sigInfo[1];
  signature.append("::");
  signature.append(sigInfo[2]);

  // Create a struct holding a jobject instance, jmethodId id and other needed thing for callback
  // Pass it to void * data to register_method
  // FIXME jobj is not a global ref, it may be invalid when it will be used
  data = new qi_method_info(instance, signature, jobj);
  gInfoHandler.push(data);


  try {
    qi::SignalLink link =obj.connect(event,
                        qi::SignalSubscriber(
                          qi::AnyFunction::fromDynamicFunction(
                            boost::bind(&event_callback_to_java, (void*) data, _1))));
    return link;
  } catch (std::exception& e)
  {
    throwNewRuntimeException(env, e.what());
    return 0;
  }
}

JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_AnyObject_connectSignal(JNIEnv *env, jobject QI_UNUSED(obj), jlong pObject, jstring jSignalName, jobject listener)
{
  qi::AnyObject *anyObject = reinterpret_cast<qi::AnyObject *>(pObject);
  std::string signalName = qi::jni::toString(jSignalName);
  auto gListener = qi::jni::makeSharedGlobalRef(env, listener);

  qi::SignalSubscriber subscriber {
    qi::AnyFunction::fromDynamicFunction(
      [gListener](const std::vector<qi::AnyReference> &params) -> qi::AnyReference {
        jobject listener = gListener.get();

        qi::jni::JNIAttach attach;
        JNIEnv *env = attach.get();

        const char *method = "onSignalReceived";
        const char *methodSig = "([Ljava/lang/Object;)V";
        jobjectArray jparams = qi::jni::toJobjectArray(params);
        qi::jni::Call<void>::invoke(env, listener, method, methodSig, jparams);
        env->DeleteLocalRef(jparams);
        jthrowable exception = env->ExceptionOccurred();
        if (exception)
        {
          env->ExceptionDescribe();
          // an exception occurred in a listener, report and ignore
          env->ExceptionClear();
        }
        return {}; // a void AnyReference
      }
    )
  };

  qi::Future<qi::SignalLink> signalLinkFuture = anyObject->connect(signalName, subscriber);

  qi::Future<qi::AnyValue> future = qi::toAnyValueFuture(std::move(signalLinkFuture));
  auto futurePtr = new qi::Future<qi::AnyValue>(std::move(future));
  return reinterpret_cast<jlong>(futurePtr);
}

JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_AnyObject_disconnectSignal(JNIEnv *env, jobject QI_UNUSED(obj), jlong pObject, jlong subscriberId)
{
  qi::AnyObject *anyObject = reinterpret_cast<qi::AnyObject *>(pObject);
  qi::Future<void> disconnectFuture = anyObject->disconnect(subscriberId);

  qi::Future<qi::AnyValue> future = qi::toAnyValueFuture(std::move(disconnectFuture));
  auto futurePtr = new qi::Future<qi::AnyValue>(std::move(future));
  return reinterpret_cast<jlong>(futurePtr);
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_AnyObject_post(JNIEnv *env, jobject QI_UNUSED(jobj), jlong pObject, jstring eventName, jobjectArray jargs)
{
  qi::AnyObject obj = *(reinterpret_cast<qi::AnyObject *>(pObject));
  std::string   event = qi::jni::toString(eventName);
  qi::GenericFunctionParameters params;
  std::string signature;
  jsize size;
  jsize i = 0;

  qi::jni::JNIAttach attach(env);

  size = env->GetArrayLength(jargs);
  i = 0;
  while (i < size)
  {
    jobject current = env->GetObjectArrayElement(jargs, i);
    qi::AnyReference val = qi::AnyReference(AnyValue_from_JObject(current).first);
    params.push_back(val);
    i++;
  }

  // Signature construction
  signature = event + "::(";
  for (unsigned i=0; i< params.size(); ++i)
    signature += params[i].signature(true).toString();
  signature += ")";

  try {
    obj.metaPost(event, params);
  } catch (std::exception& e)
  {
    throwNewException(env, e.what());
  }

  // Destroy arguments
  i = 0;
  for(qi::GenericFunctionParameters::iterator it = params.begin(); it != params.end(); ++it)
    (*it).destroy();
  return;
}

JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_AnyObject_decodeJSON(JNIEnv *QI_UNUSED(env), jclass QI_UNUSED(cls), jstring what)
{
  std::string str = qi::jni::toString(what);
  qi::AnyValue val = qi::decodeJSON(str);
  return JObject_from_AnyValue(val.asReference());
}

JNIEXPORT jstring JNICALL Java_com_aldebaran_qi_AnyObject_encodeJSON(JNIEnv *QI_UNUSED(env), jclass QI_UNUSED(cls), jobject what)
{
  std::string res = qi::encodeJSON(what);
  return qi::jni::toJstring(res);
}
