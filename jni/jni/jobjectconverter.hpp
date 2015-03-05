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
std::pair<qi::AnyReference, bool> AnyValue_from_JObject(jobject val);

#endif // !_JOBJECTCONVERTER_HPP_
