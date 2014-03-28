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

static void call_from_java_cont(qi::Future<qi::AnyReference> ret,
    qi::Promise<qi::AnyValue> promise)
{
  if (ret.hasError())
    promise.setError(ret.error());
  else
    promise.setValue(qi::AnyValue(ret.value(), false, true));
}

/**
 * @brief call_from_java Helper function to call qiMessaging method with Java arguments
 * @param env JNI environment given by JVM.
 * @param object The proxy making the call
 * @param strMethodName Name (with or without signature) of the method to call
 * @param listParams List of Java parameters given for call
 * @return
 */
qi::Future<qi::AnyValue>* call_from_java(JNIEnv *env, qi::AnyObject object, const std::string& strMethodName, jobjectArray listParams)
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
    params.push_back(qi::AnyReference::from(objs[i]));
    ++i;
  }
  // Create future and start metacall
  // philippe: must be sync or testCallback is broken (future from metacall is
  // sync, don't know why)
  qi::Promise<qi::AnyValue> promise(qi::FutureCallbackType_Sync);
  qi::Future<qi::AnyValue> *fut = new qi::Future<qi::AnyValue>();
  try
  {
    qi::Future<qi::AnyReference> metfut =
      object.metaCall(strMethodName, params);
    metfut.connect(call_from_java_cont, _1, promise);
    *fut = promise.future();
  } catch (std::runtime_error &e)
  {
    delete fut;
    throwJavaError(env, e.what());
    return 0;
  }
  return fut;
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

  qi::jni::JNIAttach attach;
  env = attach.get();

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
  std::vector<qi::TypeInterface*> types;
  for(; it != end; it++)
  {
    jvalue value;
    value.l = JObject_from_AnyValue(*it);
    qiLogVerbose() << "Converted argument " << (it-params.begin()) << it->type()->infoString();
    if (it->kind() == qi::TypeKind_Dynamic)
    {
      qiLogVerbose() << "Argument is " << (**it).type()->infoString();
      types.push_back((**it).type());
    }
    else
      types.push_back(it->type());
    args[index] = value;
    index++;
  }

  // Check if function is callable
  qi::Signature from = qi::makeTupleSignature(types);
  qi::Signature to = qi::Signature(sigInfo[2]);
  if (from.isConvertibleTo(to) == 0)
  {
    std::ostringstream ss;
    ss << "cannot convert parameters from " << from.toString() << " to " << to.toString();
    qiLogVerbose() << ss;
    throw std::runtime_error(ss.str());
  }

  // Find method class and get methodID
  cls = qi::jni::clazz(info->instance);
  if (!cls)
  {
    qiLogError() << "Service class not found";
    throw std::runtime_error("Service class not found");
  }

  // Find method ID
  std::string javaSignature = toJavaSignature(signature);
  qiLogVerbose() << "looking for method " << signature << " -> " << javaSignature;
  jmethodID mid = env->GetMethodID(cls, sigInfo[1].c_str(), javaSignature.c_str());
  if (!mid)
    mid = env->GetStaticMethodID(cls, sigInfo[1].c_str(), javaSignature.c_str());
  if (!mid)
  {
    qiLogError() << "Cannot find java method " << sigInfo[1] << javaSignature.c_str();
    throw std::runtime_error("Cannot find method");
  }

  // Call method
  qiLogVerbose() << "Entering call";
  jobject ret = env->CallObjectMethodA(info->instance, mid, args);
  qiLogVerbose() << "Finished call";

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
  if (sigInfo[0] == "" || sigInfo[0] == "v")
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
