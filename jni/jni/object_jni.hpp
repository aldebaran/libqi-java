/*
**  Copyright (C) 2018 SoftBank Robotics Europe
**  See COPYING for the license
*/

#ifndef QI_JNI_OBJECT_JNI_HPP
#define QI_JNI_OBJECT_JNI_HPP

#include <jni.h>
#include <qi/anyobject.hpp>
#include "jnitools.hpp"

namespace qi
{
namespace jni
{

/// Helper for conversion between qi::AnyObject and com.aldebaran.qi.AnyObject
class Object
{
public:
  Object(const AnyObject& o, boost::optional<JNIEnv&> env = {});

  Object(jobject value, boost::optional<JNIEnv&> env = {});

  Object(std::unique_ptr<AnyObject> o, boost::optional<JNIEnv&> env = {});

  // Copyable
  Object(const Object& o);
  Object& operator=(const Object& o);

  // Movable
  Object(Object&& o);
  Object& operator=(Object&& o);

  ~Object();

  void reset();

  /// Returns a new global reference to the internal Java object. The caller is responsible of the
  /// reference and must release it itself.
  jobject javaObject();

  AnyObject anyObject();

private:
  jobject moveRef(jobject& ref) const;
  jobject copyRef(jobject ref) const;

  void reset(boost::optional<JNIEnv&> env);

  JNIEnv* _env; // TODO: Make a wrapper for environment.
  jobject _javaObject = nullptr; // TODO: Make a wrapper for global refs.
};

} // namespace jni
} // namespace qi

#endif // !QI_JNI_OBJECT_JNI_HPP
