#include "property.hpp"
#include <qi/property.hpp>
#include <qi/future.hpp>
#include "jnitools.hpp"

namespace
{
  // Gets the type interface from a given property value class. If it fails to get it, throws a
  // Java RuntimeException and returns nullptr.
  qi::TypeInterface* getPropertyValueType(JNIEnv* env, jclass valueClass)
  {
    auto* const valueType = propertyValueClassToType(env, valueClass);
    if (!valueType)
    {
      const std::string message =
          "Could not find the qi.TypeInterface of property value class of signature '" +
          propertyBaseSignature(env, valueClass) + "'";
      throwNewRuntimeException(env, message.c_str());
    }
    return valueType;
  }
}

jlong JNICALL Java_com_aldebaran_qi_Property_createProperty(JNIEnv* env,
                                                            jclass QI_UNUSED(propertyClass),
                                                            jclass valueClass)
{
  try
  {
    auto* const valueType = getPropertyValueType(env, valueClass);
    if (!valueType) return 0;
    auto* const propertyManager = new PropertyManager(*valueType);
    return reinterpret_cast<jlong>(propertyManager);
  }
  catch (std::exception& e)
  {
    throwNewException(env, e.what());
  }
  return 0;
}

jlong JNICALL
Java_com_aldebaran_qi_Property_createPropertyWithValue(JNIEnv* env,
                                                       jclass QI_UNUSED(propertyClass),
                                                       jclass valueClass,
                                                       jobject value)
{
  try
  {
    // TODO: Handle this case in the conversion between AnyValue and jobject.
    if (env->IsSameObject(value, nullptr))
    {
      throwNewNullPointerException(env);
      return 0;
    }

    auto* const  valueType = getPropertyValueType(env, valueClass);
    if (!valueType) return 0;
    auto* const propertyManager = new PropertyManager(*valueType, *env, value);
    return reinterpret_cast<jlong>(propertyManager);
  }
  catch (std::exception& e)
  {
    throwNewException(env, e.what());
  }
  return 0;
}

jlong JNICALL Java_com_aldebaran_qi_Property_get(JNIEnv* env,
                                                 jobject QI_UNUSED(propertyObj),
                                                 jlong pointer)
{
  try
  {
    auto propertyManager = reinterpret_cast<PropertyManager *>(pointer);
    const auto& propertyPointer = propertyManager->property;
    // Potential global reference issue here, due to multithreading, garbage collector and other fun.
    auto futurePointer = new qi::Future<qi::AnyValue> { propertyPointer->value().async() };
    return reinterpret_cast<jlong>(futurePointer);
  }
  catch (std::exception& e)
  {
    throwNewException(env, e.what());
  }
  return 0;
}

jlong JNICALL Java_com_aldebaran_qi_Property_set(JNIEnv* env,
                                                 jobject QI_UNUSED(propertyObj),
                                                 jlong pointer,
                                                 jobject value)
{
  try
  {
    auto propertyManager = reinterpret_cast<PropertyManager*>(pointer);
    propertyManager->setValue(env, value);
  }
  catch (std::exception& e)
  {
    throwNewException(env, e.what());
  }
  // Return a jlong to keep the compatibility with the Java side.
  return 0;
}

void JNICALL Java_com_aldebaran_qi_Property_destroy(JNIEnv* env,
                                                    jobject QI_UNUSED(propertyObj),
                                                    jlong pointer)
{
  try
  {
    auto propertyManager = reinterpret_cast<PropertyManager*>(pointer);
    propertyManager->destroy(env);
    delete propertyManager;
  }
  catch (std::exception& e)
  {
    throwNewException(env, e.what());
  }
}
