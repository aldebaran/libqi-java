/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#pragma once
#ifndef _JOBJECTCONVERTER_HPP_
#define _JOBJECTCONVERTER_HPP_

#include <jni.h>
#include <qi/type/typeinterface.hpp>

jobject JObject_from_AnyValue(qi::AnyReference val);
void JObject_from_AnyValue(qi::AnyReference val, jobject* target);

/**
 * @brief
 *   Converts a Java object into a qi.AnyReference.
 * @param hintSig
 *   A hint on the signature of the desired type to guide the conversion. It is ignored if the
 *   signature is not valid.
 * @returns
 *   A pair where the first member is a reference to the converted value and the second member
 *   indicates that the reference is owning the value and that the caller must explicitly destroy
 *   it.
 */
std::pair<qi::AnyReference, bool> AnyValue_from_JObject(jobject val,
                                                        qi::Signature signatureHint = {});

namespace qi
{
namespace jni
{

using AnyValueMap = std::map<std::string, AnyValue>;

// Converts a Java map of String to Object into a C++ map.
AnyValueMap toCppStringAnyValueMap(JNIEnv& env, jobject jmap);

// Returns a local reference to a Java map of String to Object. The caller is responsible for
// releasing it.
jobject toJavaStringObjectMap(JNIEnv& env, const AnyValueMap& map);

} // namespace jni
} // namespace qi

#endif // !_JOBJECTCONVERTER_HPP_
