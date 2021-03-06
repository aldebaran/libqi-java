﻿/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#include <map>
#include <qi/log.hpp>
#include <qi/type/metamethod.hpp>
#include <qi/type/dynamicobjectbuilder.hpp>
#include <qi/anyobject.hpp>
#include <qi/anyfunction.hpp>
#include <qi/property.hpp>
#include <jni/jnitools.hpp>
#include <jni/object_jni.hpp>
#include <jni/callbridge.hpp>
#include <jni/objectbuilder.hpp>

JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_DynamicObjectBuilder_create(JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj))
{
  qi::DynamicObjectBuilder *ob = new qi::DynamicObjectBuilder();
  return (jlong) ob;
}

JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_DynamicObjectBuilder_object(JNIEnv *env, jobject QI_UNUSED(jobj), jlong pObjectBuilder)
{
  const auto ob = reinterpret_cast<qi::DynamicObjectBuilder *>(pObjectBuilder);
  try
  {
    return qi::jni::createAnyObject(ob->object(), *env);
  }
  catch (const std::runtime_error& e)
  {
    throwNewRuntimeException(env, e.what());
  }
  return nullptr;
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_DynamicObjectBuilder_destroy(JNIEnv *env, jobject QI_UNUSED(obj), jlong pObjectBuilder)
{
  qi::DynamicObjectBuilder *ob = reinterpret_cast<qi::DynamicObjectBuilder *>(pObjectBuilder);
  delete ob;
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_DynamicObjectBuilder_advertiseMethod(
    JNIEnv *env, jobject jobj, jlong pObjectBuilder,
    jstring method, jobject instance, jstring className, jstring desc)
{
  extern MethodInfoHandler   gInfoHandler;
  qi::DynamicObjectBuilder  *ob = reinterpret_cast<qi::DynamicObjectBuilder *>(pObjectBuilder);
  std::string                signature = qi::jni::toString(method);
  qi_method_info*            data;
  std::vector<std::string>   sigInfo;
  std::string                description = qi::jni::toString(desc);

  // Create a new global reference on object instance.
  // jobject structure are local reference and are destroyed when returning to JVM
  // Fixme : May leak global ref.
  instance = env->NewGlobalRef(instance);

  // Create a struct holding a jobject instance, jmethodId id and other needed thing for callback
  // Pass it to void * data to register_method
  // In java_callback, use it directly so we don't have to find method again
  // FIXME jobj is not a global ref, it may be invalid when it will be used
  data = new qi_method_info(instance, signature, jobj);
  gInfoHandler.push(data);

  try
  {
    // Bind method signature on generic java callback
    sigInfo = qi::signatureSplit(signature);

    auto callToJava = [signature, data](const qi::GenericFunctionParameters& params)
    {
      return call_to_java(signature, data, params);
    };

    ob->xAdvertiseMethod(
          sigInfo[0], sigInfo[1], sigInfo[2],
          qi::AnyFunction::fromDynamicFunction(callToJava).dropFirstArgument(),
          description);
  }
  catch (std::runtime_error &e)
  {
    throwNewAdvertisementException(env, e.what());
  }
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_DynamicObjectBuilder_advertiseSignal(JNIEnv *env, jobject QI_UNUSED(obj), jlong pObjectBuilder, jstring eventSignature)
{
  qi::DynamicObjectBuilder  *ob = reinterpret_cast<qi::DynamicObjectBuilder *>(pObjectBuilder);

  try
  {
    std::vector<std::string>   sigInfo = qi::signatureSplit(qi::jni::toString(eventSignature));
    std::string   event = sigInfo[1];
    std::string   callbackSignature = sigInfo[0] + sigInfo[2];
    ob->xAdvertiseSignal(event, callbackSignature);
  }
  catch (std::runtime_error &e)
  {
    throwNewAdvertisementException(env, e.what());
  }
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_DynamicObjectBuilder_advertiseProperty(JNIEnv *env, jobject QI_UNUSED(obj), jlong pObjectBuilder, jstring jname, jclass propertyBase)
{
  qi::DynamicObjectBuilder  *ob = reinterpret_cast<qi::DynamicObjectBuilder *>(pObjectBuilder);
  std::string                name = qi::jni::toString(jname);

  std::string sig = propertyBaseSignature(env, propertyBase);
  try {
    ob->xAdvertiseProperty(name, sig);
  }
  catch (std::runtime_error &e)
  {
    throwNewAdvertisementException(env, e.what());
  }
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_DynamicObjectBuilder_setThreadSafeness(JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pObjectBuilder, jboolean isThreadSafe)
{
  qi::DynamicObjectBuilder  *ob = reinterpret_cast<qi::DynamicObjectBuilder *>(pObjectBuilder);
  ob->setThreadingModel(isThreadSafe?qi::ObjectThreadingModel_MultiThread:qi::ObjectThreadingModel_SingleThread);
}

/**
 * Advertise a property with property object
 * @param env JNI environment
 * @param clazz DynamicObjectBuilder class
 * @param pObjectBuilder Pointer on DynamicObjectBuilder instance
 * @param name Property name
 * @param pointerProperty Pointer on property instance
 */
JNIEXPORT void JNICALL Java_com_aldebaran_qi_DynamicObjectBuilder_advertisePropertyObject(JNIEnv * env, jclass clazz, jlong pObjectBuilder, jstring name, jlong pointerProperty)
{
  qi::DynamicObjectBuilder  *dynamicObjectBuilder = reinterpret_cast<qi::DynamicObjectBuilder *>(pObjectBuilder);
  auto propertyManager = reinterpret_cast<PropertyManager *>(pointerProperty);
  std::string propertyName = qi::jni::toString(name);

  try
  {
    dynamicObjectBuilder -> advertiseProperty(propertyName, propertyManager->property.get());
  }
  catch (std::runtime_error &e)
  {
    throwNewAdvertisementException(env, e.what());
  }
}
