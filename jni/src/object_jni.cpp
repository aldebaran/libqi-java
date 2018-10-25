/*
**  Copyright (C) 2018 SoftBank Robotics Europe
**  See COPYING for the license
*/

#include <stdexcept>
#include <sstream>
#include <qi/log.hpp>
#include <qi/type/dynamicobject.hpp>
#include "object_jni.hpp"

qiLogCategory("qi.jni.object");

namespace qi
{
namespace jni
{

namespace
{

boost::optional<JNIEnv&> findEnv(boost::optional<JNIEnv&> env = {})
{
  if (env)
    return *env;
  if (auto* const globalEnv = qi::jni::env())
    return *globalEnv;
  return {};
}

JNIEnv& getEnv(boost::optional<JNIEnv&> aenv = {})
{
  auto env = findEnv(aenv);
  if (!env)
    throw std::runtime_error("Failed to get a valid JNI environment.");
  return *env;
}

}

jobject createAnyObject(std::unique_ptr<qi::AnyObject> object, boost::optional<JNIEnv&> aenv)
{
  if (!object || !object->isValid())
    return nullptr;

  auto& env = getEnv(aenv);
  const auto methodId = env.GetMethodID(cls_anyobject, "<init>", "(J)V");
  if (!methodId)
  {
    const auto msg = "qi::jni::AnyObject: Cannot find AnyObject constructor.";
    qiLogError() << msg;
    throw std::runtime_error(msg);
  }

  // Release the `qi::AnyObject`, its lifetime will be bound to the Java object.
  const auto internalPtr = reinterpret_cast<jlong>(object.release());
  return env.NewObject(cls_anyobject, methodId, internalPtr);
}

jobject createAnyObject(const qi::AnyObject& o, boost::optional<JNIEnv&> env)
{
  return o.isValid() ? createAnyObject(std::unique_ptr<qi::AnyObject>(new auto(o)), env) : nullptr;
}

qi::AnyObject internalQiAnyObject(jobject jAnyObject, boost::optional<JNIEnv&> aenv)
{
  auto& env = getEnv(aenv);
  if (env.IsSameObject(jAnyObject, nullptr))
    return {};

  const auto fieldId = env.GetFieldID(cls_anyobject, "_p", "J");
  if (!fieldId)
  {
    const auto msg = "qi::jni::AnyObject: Cannot get AnyObject Java object internal native pointer.";
    qiLogWarning() << msg;
    throw std::runtime_error{ msg };
  }

  const auto internalPtr = env.GetLongField(jAnyObject, fieldId);
  auto* const qiAnyObj = reinterpret_cast<const qi::AnyObject*>(internalPtr);
  return qiAnyObj ? *qiAnyObj : qi::AnyObject();
}

} // namespace jni
} // namespace qi
