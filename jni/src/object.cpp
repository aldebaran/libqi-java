/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#include <cstdint>
#include <type_traits>

#include <qi/anyobject.hpp>
#include <qi/jsoncodec.hpp>

#include <jni/jnitools.hpp>
#include <jni/object.hpp>
#include <jni/callbridge.hpp>
#include <jni/jobjectconverter.hpp>

qiLogCategory("qimessaging.jni");

extern MethodInfoHandler gInfoHandler;

JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_AnyObject_property(JNIEnv* env, jobject QI_UNUSED(jobj), jlong pObj, jstring name)
{
  qi::AnyObject&     obj = *(reinterpret_cast<qi::AnyObject*>(pObj));
  if (!qi::jni::assertion(env, obj.isValid(), "AnyObject.property: Invalid qi.AnyObject."))
    return 0;

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
  if (!qi::jni::assertion(env, objectPtr->isValid(),
                          "AnyObject.setProperty: Invalid qi.AnyObject."))
    return 0;

  try
  {
    // We get the signature as a hint for conversion.
    auto propertyName = qi::jni::toString(jPropertyName);
    const auto& metaObject = objectPtr->metaObject();
    const auto propertyId = metaObject.propertyId(propertyName);
    if (propertyId == -1)
    {
      return reinterpret_cast<jlong>(new auto(qi::makeFutureError<qi::AnyValue>(
        "object as no such property \""+ propertyName + std::string("\"")
      )));
    }
    const auto* metaProperty = metaObject.property(propertyId);
    QI_ASSERT(metaProperty);
    const auto signatureHint = metaProperty->signature();

    // Performing the conversion.
    qi::jni::JNIAttach attach(env);
    auto conversionResult = AnyValue_from_JObject(jValue, signatureHint);
    if(!conversionResult.second)
    {
      return reinterpret_cast<jlong>(new auto(qi::makeFutureError<qi::AnyValue>(
          std::string("while setting property \"") + propertyName +
          "\", could not convert JObject to AnyValue of signature " +
          signatureHint.toPrettySignature())));
    }

    // Setting the property and wrapping the future result.
    std::unique_ptr<qi::Future<qi::AnyValue>> futurePtr{new qi::Future<qi::AnyValue>()};
    *futurePtr = qi::toAnyValueFuture(objectPtr->setProperty(propertyName, conversionResult.first).async());
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
  if (!qi::jni::assertion(env, obj.isValid(), "AnyObject.asyncCall: Invalid qi.AnyObject."))
    return 0;

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

JNIEXPORT jstring JNICALL Java_com_aldebaran_qi_AnyObject_metaObjectToString(JNIEnv* env, jobject QI_UNUSED(jobj), jlong pObject)
{
  qi::AnyObject&    obj = *(reinterpret_cast<qi::AnyObject*>(pObject));
  if (!qi::jni::assertion(env, obj.isValid(),
                          "AnyObject.metaObjectToString: Invalid qi.AnyObject."))
    return nullptr;

  std::ostringstream ss;
  qi::detail::printMetaObject(ss, obj.metaObject(), false /* do not print colors */);
  return qi::jni::toJstring(ss.str());
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_AnyObject_destroy(JNIEnv* QI_UNUSED(env), jobject QI_UNUSED(jobj), jlong pObject)
{
  qi::AnyObject*    obj = reinterpret_cast<qi::AnyObject*>(pObject);

  delete obj;
}

JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_AnyObject_connectSignal(JNIEnv *env, jobject QI_UNUSED(obj), jlong pObject, jstring jSignalName, jobject listener)
{
  qi::AnyObject *anyObject = reinterpret_cast<qi::AnyObject *>(pObject);
  if (!qi::jni::assertion(env, anyObject->isValid(),
                          "AnyObject.connectSignal: Invalid qi.AnyObject."))
    return 0;

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
  if (!qi::jni::assertion(env, anyObject->isValid(),
                          "AnyObject.disconnectSignal: Invalid qi.AnyObject."))
    return 0;

  qi::Future<void> disconnectFuture = anyObject->disconnect(subscriberId);

  qi::Future<qi::AnyValue> future = qi::toAnyValueFuture(std::move(disconnectFuture));
  auto futurePtr = new qi::Future<qi::AnyValue>(std::move(future));
  return reinterpret_cast<jlong>(futurePtr);
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_AnyObject_post(JNIEnv *env, jobject QI_UNUSED(jobj), jlong pObject, jstring eventName, jobjectArray jargs)
{
  qi::AnyObject obj = *(reinterpret_cast<qi::AnyObject *>(pObject));
  if (!qi::jni::assertion(env, obj.isValid(), "AnyObject.post: Invalid qi.AnyObject."))
    return;

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

/**
 * @brief Compare 2 AnyObject.
 * It returns -1, if object1 < object2
 * It returns 0, if object1 == object2
 * It returns 1, if object1 > object2
 * @param env JNI evironment
 * @param cls Class reference
 * @param object1 Object1 memory address
 * @param object2 Object2 memory address
 * @return Comparison result
 */
JNIEXPORT jint JNICALL Java_com_aldebaran_qi_AnyObject_compare(JNIEnv * env, jclass cls, jlong object1, jlong object2)
{
  qi::AnyObject anyObject1 = *(reinterpret_cast<qi::AnyObject *>(object1));
  qi::AnyObject anyObject2 = *(reinterpret_cast<qi::AnyObject *>(object2));

  if (!qi::jni::assertion(env, anyObject1.isValid(),
                          "AnyObject.compare: Invalid first qi.AnyObject.") ||
      !qi::jni::assertion(env, anyObject2.isValid(),
                          "AnyObject.compare: Invalid second qi.AnyObject."))
    return 0;

  if(anyObject1 == anyObject2)
  {
    return 0;
  }

  if(anyObject1 < anyObject2)
  {
    return -1;
  }

  return 1;
}

template<typename T>
using is_signed_integral = ka::Conjunction<std::is_signed<T>, std::is_integral<T>>;

/**
 * @brief Converts a C++ hash to a java hash.
 */
static jint toJavaHash(std::size_t hash)
{
  static_assert(sizeof(std::uint64_t) >= sizeof(std::size_t), "uint64_t is unable to store the size_t value.");

  const std::uint64_t hash64 = hash;

  // Follow implementation of Long::hashCode() in oracle documentation.
  // See https://docs.oracle.com/javase/7/docs/api/java/lang/Long.html#hashCode()
  const auto hash32 = static_cast<std::uint32_t>((hash64 & 0x00000000ffffffff) ^ (hash64 >> 32));

  static_assert(alignof(jint) == alignof(std::uint32_t), "Invalid alignment between jint and uint32_t, cannot reinterpret cast.");

  static_assert(is_signed_integral<jint>::value && sizeof(jint) == sizeof(std::int32_t), "jint is not a signed 32 bit integral.");

  return reinterpret_cast<const jint&>(hash32);
}

/**
 * @brief Hashes an AnyObject.
 * Returns a java compliant hash value.
 * If the pointer is null or the dereferenced object is invalid, report AssertionError and return 0
 */
JNIEXPORT jint JNICALL Java_com_aldebaran_qi_AnyObject_hash(JNIEnv* env, jclass QI_UNUSED(cls), jlong object)
{
  const auto anyObjPtr = reinterpret_cast<const qi::AnyObject*>(object);

  if (!qi::jni::assertion(env, anyObjPtr != nullptr,
                          "AnyObject.hash: Null qi.AnyObject pointer."))
    return 0;

  const auto& anyObject = *anyObjPtr;

  if (!qi::jni::assertion(env, anyObject.isValid(), 
                          "AnyObject.hash: Invalid qi.AnyObject."))
    return 0;

  return toJavaHash(std::hash<qi::AnyObject>{}(anyObject));
}

