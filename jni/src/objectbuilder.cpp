/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
*/

#include <map>
#include <qi/log.hpp>
#include <qi/type/metamethod.hpp>
#include <qi/type/dynamicobjectbuilder.hpp>
#include <qi/anyobject.hpp>
#include <qi/anyfunction.hpp>
#include <jnitools.hpp>

#include <object_jni.hpp>
#include <callbridge.hpp>
#include <objectbuilder.hpp>

jlong   Java_com_aldebaran_qi_GenericObject_qiObjectBuilderCreate(JNIEnv* env, jobject QI_UNUSED(jobj))
{
  // Keep a pointer on JavaVM singleton if not already set.
  JVM(env);

  qi::DynamicObjectBuilder *ob = new qi::DynamicObjectBuilder();
  return (jlong) ob;
}

jlong   Java_com_aldebaran_qi_GenericObject_qiObjectBuilderGetObject(JNIEnv *env, jobject jobj, jlong pObjectBuilder)
{
  qi::DynamicObjectBuilder *ob = reinterpret_cast<qi::DynamicObjectBuilder *>(pObjectBuilder);
  qi::AnyObject *obj = new qi::AnyObject();
  qi::AnyObject &o = *(reinterpret_cast<qi::AnyObject *>(obj));

  o = ob->object();
  return (jlong) obj;
}

void   Java_com_aldebaran_qi_GenericObject_qiObjectBuilderDestroy(long pObjectBuilder)
{
  qi::DynamicObjectBuilder *ob = reinterpret_cast<qi::DynamicObjectBuilder *>(pObjectBuilder);
  delete ob;
}

jlong   Java_com_aldebaran_qi_DynamicObjectBuilder_create()
{
  qi::DynamicObjectBuilder *ob = new qi::DynamicObjectBuilder();
  return (jlong) ob;
}

jobject Java_com_aldebaran_qi_DynamicObjectBuilder_object(JNIEnv* env, jobject QI_UNUSED(jobj), jlong pObjectBuilder)
{
  qi::DynamicObjectBuilder *ob = reinterpret_cast<qi::DynamicObjectBuilder *>(pObjectBuilder);
  qi::AnyObject *obj = new qi::AnyObject();
  qi::AnyObject &o = *(reinterpret_cast<qi::AnyObject *>(obj));

  JVM(env);
  o = ob->object();

  JNIObject jobj(obj);
  return jobj.object();
}

void    Java_com_aldebaran_qi_DynamicObjectBuilder_destroy(JNIEnv *env, jobject jobj, jlong pObjectBuilder)
{
  qi::DynamicObjectBuilder *ob = reinterpret_cast<qi::DynamicObjectBuilder *>(pObjectBuilder);
  delete ob;
}

jlong   Java_com_aldebaran_qi_DynamicObjectBuilder_advertiseMethod(JNIEnv *env, jobject jobj, jlong pObjectBuilder, jstring method, jobject instance, jstring className, jstring desc)
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
  data = new qi_method_info(instance, signature, jobj);
  gInfoHandler.push(data);

  // Bind method signature on generic java callback
  sigInfo = qi::signatureSplit(signature);
  int ret = ob->xAdvertiseMethod(sigInfo[0],
                                 sigInfo[1],
                                 sigInfo[2],
                                 qi::AnyFunction::fromDynamicFunction(boost::bind(&call_to_java, signature, data, _1)).dropFirstArgument(),
                                 description);

  return (jlong) ret;
}

jlong   Java_com_aldebaran_qi_DynamicObjectBuilder_advertiseSignal(JNIEnv *env, jobject obj, jlong pObjectBuilder, jstring eventSignature)
{
  qi::DynamicObjectBuilder  *ob = reinterpret_cast<qi::DynamicObjectBuilder *>(pObjectBuilder);
  std::vector<std::string>   sigInfo = qi::signatureSplit(qi::jni::toString(eventSignature));
  std::string   event = sigInfo[1];
  std::string   callbackSignature = sigInfo[0] + sigInfo[2];

  return (jlong) ob->xAdvertiseSignal(event, callbackSignature);
}

jlong   Java_com_aldebaran_qi_DynamicObjectBuilder_advertiseProperty(JNIEnv *env, jobject QI_UNUSED(obj), jlong pObjectBuilder, jstring jname, jclass propertyBase)
{
  qi::DynamicObjectBuilder  *ob = reinterpret_cast<qi::DynamicObjectBuilder *>(pObjectBuilder);
  std::string                name = qi::jni::toString(jname);

  std::string sig = propertyBaseSignature(env, propertyBase);
  return (jlong) ob->xAdvertiseProperty(name, sig);
}


jlong Java_com_aldebaran_qi_DynamicObjectBuilder_advertiseThreadSafeness(JNIEnv *env, jobject obj, jlong pObjectBuilder, jboolean isThreadSafe)
{
  qi::DynamicObjectBuilder  *ob = reinterpret_cast<qi::DynamicObjectBuilder *>(pObjectBuilder);
  ob->setThreadingModel(isThreadSafe?qi::ObjectThreadingModel_MultiThread:qi::ObjectThreadingModel_SingleThread);
  return 1;
}
