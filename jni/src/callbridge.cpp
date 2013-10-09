/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
*/

#include <qi/log.hpp>
#include <qi/future.hpp>

#include <qitype/anyobject.hpp>
#include <qitype/dynamicobjectbuilder.hpp>
#include <qitype/anyfunction.hpp>

#include <callbridge.hpp>
#include <jobjectconverter.hpp>
#include <jnitools.hpp>

qiLogCategory("qimessaging.jni");

MethodInfoHandler gInfoHandler;

/**
 * @brief call_from_java Helper function to call qiMessaging method with Java arguments
 * @param env JNI environment given by JVM.
 * @param object The proxy making the call
 * @param strMethodName Name (with or without signature) of the method to call
 * @param listParams List of Java parameters given for call
 * @return
 */
qi::Future<qi::AnyReference>* call_from_java(JNIEnv *env, qi::AnyObject object, const std::string& strMethodName, jobjectArray listParams)
{
  qi::GenericFunctionParameters params;
  jsize size;
  jsize i = 0;

  size = env->GetArrayLength(listParams);
  // We need to take references on jobjects, so they can't move while we make the call
  std::vector<jobject> objs;
  objs.resize(size);
  while (i < size)
  {
    jobject current = env->GetObjectArrayElement(listParams, i);
    objs[i] = current;
    qi::AnyReference val(objs[i]);
    params.push_back(val);
    ++i;
  }
  // Create future and start metacall
  qi::Future<qi::AnyReference> *fut = new qi::Future<qi::AnyReference>();
  try
  {
    *fut = object.metaCall(strMethodName, params);
    return fut;
  } catch (std::runtime_error &e)
  {
    throwJavaError(env, e.what());
    return 0;
  }
}

/**
 * @brief call_to_java Heller function to call Java methods.
 * @param signature qitype signature formated
 * @param data pointer on a qi_method_info (which hold Java object class and reference)
 * @param params parameters to forward to called method
 * @return
 */
qi::AnyReference call_to_java(std::string signature, void* data, const qi::GenericFunctionParameters& params)
{
  qi::AnyReference res;
  jvalue*             args = new jvalue[params.size()];
  int                 index = 0;
  JNIEnv*             env = 0;
  qi_method_info*     info = reinterpret_cast<qi_method_info*>(data);
  jclass              cls = 0;
  std::vector<std::string>  sigInfo = qi::signatureSplit(signature);

  // Attach JNIEnv to current thread to avoid segfault in jni functions. (eventloop dependent)
  if (JVM()->AttachCurrentThread((envPtr) &env, (void *) 0) != JNI_OK || env == 0)
  {
    qiLogError() << "Cannot attach callback thread to Java VM";
    throwJavaError(env, "Cannot attach callback thread to Java VM");
    return res;
  }

  // Check value of method info structure
  if (info == 0)
  {
    qiLogError() << "Internal method informations are not valid";
    throwJavaError(env, "Internal method informations are not valid");
    return res;
  }
  // Translate parameters from AnyValues to jobjects
  qi::GenericFunctionParameters::const_iterator it = params.begin();
  qi::GenericFunctionParameters::const_iterator end = params.end();
  for(; it != end; it++)
  {
    jvalue value;
    value.l = JObject_from_AnyValue(*it);
    args[index] = value;
    index++;
  }

  // Find method class and get methodID
  cls = qi::jni::clazz(info->instance);
  if (!cls)
  {
    qiLogError() << "Service class not found";
    throw std::runtime_error("Service class not found");
  }

  // Find method ID
  jmethodID mid = env->GetMethodID(cls, sigInfo[1].c_str(), toJavaSignature(signature).c_str());
  if (!mid)
    mid = env->GetStaticMethodID(cls, sigInfo[1].c_str(), toJavaSignature(signature).c_str());
  if (!mid)
  {
    qiLogError() << "Cannot find java method " << sigInfo[1] << toJavaSignature(signature).c_str();
    throw std::runtime_error("Cannot find method");
  }

  // Call method
  jobject ret = env->CallObjectMethodA(info->instance, mid, args);

  // Did method thrown ?
  if (env->ExceptionCheck())
  {
    env->ExceptionDescribe();
    env->ExceptionClear();
    throw std::runtime_error("Remote method thrown exception");
  }

  // Release instance clazz
  qi::jni::releaseClazz(cls);

  // Release arguments
  while (--index >= 0)
    qi::jni::releaseObject(args[index].l);
  delete[] args;

  // If method return signature is void, return here
  if (sigInfo[0] == "")
    return qi::AnyReference(qi::typeOf<void>());

  // Convert return value in AnyValue
  res = AnyValue_from_JObject(ret).first;
  qi::jni::releaseObject(ret);
  return res;
}

/**
 * @brief event_callback_to_java Generic callback for all events
 * @param vinfo pointer on a qi_method_info (which hold Java object class and reference)
 * @param params parameters to forward to callback
 * @return
 */
qi::AnyReference event_callback_to_java(void *vinfo, const std::vector<qi::AnyReference>& params)
{
  qi_method_info*  info = static_cast<qi_method_info*>(vinfo);

  qiLogVerbose("qimessaging.jni") << "Java event callback called (sig=" << info->sig << ")";

  return call_to_java(info->sig, info, params);
}
