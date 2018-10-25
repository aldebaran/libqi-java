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

/// Creates a `com.aldebaran.qi.AnyObject` object with the given `qi::AnyObject` as the internal
/// pointer and returns a local reference to the object.
jobject createAnyObject(std::unique_ptr<qi::AnyObject> object, boost::optional<JNIEnv&> env = {});

/// Same as `createAnyObject(std::unique_ptr<qi::AnyObject>, boost::optional<JNIEnv&>)` but
/// allocates a copy of the object first.
jobject createAnyObject(const qi::AnyObject& object, boost::optional<JNIEnv&> env = {});

/// If the object is a non-null reference to a `com.aldebaran.qi.AnyObject` object, fetches its
/// internal pointer and returns a copy of the `qi::AnyObject`. Otherwise returns an invalid
/// `qi::AnyObject`.
///
/// Throws a std::runtime_error if it fails to access the internal pointer or if that pointer is
/// null.
qi::AnyObject internalQiAnyObject(jobject jAnyObject, boost::optional<JNIEnv&> env = {});

} // namespace jni
} // namespace qi

#endif // !QI_JNI_OBJECT_JNI_HPP
