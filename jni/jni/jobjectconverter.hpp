/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
*/

#pragma once
#ifndef _JOBJECTCONVERTER_HPP_
#define _JOBJECTCONVERTER_HPP_

#include <jni.h>
#include <qitype/type.hpp>

jobject JObject_from_GenericValue(qi::AnyReference val);
void JObject_from_GenericValue(qi::AnyReference val, jobject* target);
std::pair<qi::AnyReference, bool> GenericValue_from_JObject(jobject val);

#endif // !_JOBJECTCONVERTER_HPP_
