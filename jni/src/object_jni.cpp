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

boost::optional<JNIEnv&> optEnv(JNIEnv* env)
{
  if (env)
    return *env;
  return {};
}

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

Object::Object(const AnyObject& obj, boost::optional<JNIEnv&> env)
  : Object{  obj.isValid() ? std::unique_ptr<AnyObject>(new AnyObject{ obj }) : nullptr, env}
{
}

Object::Object(std::unique_ptr<AnyObject> object, boost::optional<JNIEnv&> aenv)
  : Object{ object && object->isValid() ?
              [&] {
                auto& env = getEnv(aenv);
                const auto methodId = env.GetMethodID(cls_anyobject, "<init>", "(J)V");
                if (!methodId)
                {
                  const auto msg = "qi::jni::Object: Cannot find AnyObject constructor.";
                  qiLogError() << msg;
                  throw std::runtime_error(msg);
                }

                // Release the AnyObject, its lifetime will be bound to the Java object.
                const auto pObj = reinterpret_cast<jlong>(object.release());
                return env.NewObject(cls_anyobject, methodId, pObj);
              }() :
            nullptr, aenv}
{
}

Object::Object(jobject value, boost::optional<JNIEnv&> aenv)
  : _env{ aenv ? &*aenv : nullptr }
  , _javaObject{ [&]() -> jobject {
    auto& env = getEnv(aenv);
    if (env.IsSameObject(value, nullptr))
      return nullptr;
    return env.NewGlobalRef(value);
  }() }
{
}

Object::Object(const Object& o)
  : _env{ o._env }
  , _javaObject{ copyRef(o._javaObject) }
{
}

Object& Object::operator=(const Object& o)
{
  if (this == &o)
    return *this;

  reset();
  _env = o._env;
  _javaObject = copyRef(o._javaObject);
  return *this;
}

Object::Object(Object&& o)
  : _env{ o._env }
  , _javaObject{ moveRef(o._javaObject) }
{
}

Object& Object::operator=(Object&& o)
{
  if (this == &o)
    return *this;

  reset();
  _env = o._env;
  _javaObject = moveRef(o._javaObject);
  return *this;
}

Object::~Object()
{
  reset(findEnv(optEnv(_env)));
}

void Object::reset()
{
  reset(getEnv(optEnv(_env)));
}

jobject Object::javaObject()
{
  return copyRef(_javaObject);
}

AnyObject Object::anyObject()
{
  auto& env = getEnv(optEnv(_env));
  if (env.IsSameObject(_javaObject, nullptr))
    return {};

  const auto fieldId = env.GetFieldID(cls_anyobject, "_p", "J");
  if (!fieldId)
  {
    const auto msg = "qi::jni::Object: Cannot get AnyObject Java object internal native pointer.";
    qiLogWarning() << msg;
    throw std::runtime_error{ msg };
  }

  auto fieldValue = env.GetLongField(_javaObject, fieldId);
  return *(reinterpret_cast<AnyObject*>(fieldValue));
}

jobject Object::moveRef(jobject& ref) const
{
  return ka::exchange(ref, nullptr);
}

jobject Object::copyRef(jobject ref) const
{
  auto& env = getEnv(optEnv(_env));
  if (env.IsSameObject(ref, nullptr))
    return nullptr;
  return env.NewGlobalRef(ref);
}

void Object::reset(boost::optional<JNIEnv&> env)
{
  if (auto obj = ka::exchange(_javaObject, nullptr))
  {
    if (env)
      env->DeleteGlobalRef(obj);
  }
}

} // namespace jni
} // namespace qi
