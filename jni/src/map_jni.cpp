/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#include <stdexcept>
#include <qi/log.hpp>

#include <jnitools.hpp>
#include <map_jni.hpp>

qiLogCategory("qimessaging.jni");

JNIMap::JNIMap()
{
  javaVirtualMachine->GetEnv((void**) &_env, QI_JNI_MIN_VERSION);

  jmethodID mid = _env->GetMethodID(cls_hashmap, "<init>", "()V");
  if (!mid)
  {
    qiLogFatal() << "JNIMap::JNIMap : Cannot call constructor";
    throw std::runtime_error("JNIMap::JNIMap : Cannot call constructor");
  }

  _obj = _env->NewObject(cls_hashmap, mid);
}

JNIMap::JNIMap(jobject obj)
{
  javaVirtualMachine->GetEnv((void**) &_env, QI_JNI_MIN_VERSION);
  _obj = obj;
}

void JNIMap::put(jobject key, jobject value)
{
  jmethodID mid = _env->GetMethodID(cls_map, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

  if (!key || !value)
  {
    qiLogFatal() << "JNIMap::put() : Given key/value pair is null";
    return;
  }

  if (!mid)
  {
    qiLogFatal() << "JNIMap::put() : Cannot call put";
    throw std::runtime_error("JNIMap::put() : Cannot call put");
  }

  _env->CallObjectMethod(_obj, mid, key, value);
}

jobject JNIMap::object()
{
  return _obj;
}

int     JNIMap::size()
{
  jmethodID mid = _env->GetMethodID(cls_map, "size", "()I");

  if (!mid) // or throw std::runtime_error ?
    return (-1);

  return _env->CallIntMethod(_obj, mid);
}

jobjectArray JNIMap::keys()
{
  jobject set = qi::jni::Call<jobject>::invoke(_env, _obj, "keySet", "()Ljava/util/Set;");
  if (!set)
    return nullptr;
  jobject asArray = qi::jni::Call<jobject>::invoke(_env, set, "toArray", "()[Ljava/lang/Object;");
  if (!asArray)
    return nullptr;
  return static_cast<jobjectArray>(asArray);
}

jobject JNIMap::get(jobject key)
{
  jmethodID mid = _env->GetMethodID(cls_map, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");

  if (!key)
  {
    qiLogFatal() << "JNIMap::get() : Given key is null";
    return 0;
  }

  if (!mid)
  {
    qiLogFatal() << "JNIMap::get() : Cannot call method get";
    throw std::runtime_error("JNIMap::get() : Cannot call method get");
  }

  return _env->CallObjectMethod(_obj, mid, key);
}

