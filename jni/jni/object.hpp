/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#ifndef _JAVA_JNI_OBJECT_HPP_
#define _JAVA_JNI_OBJECT_HPP_

#include <qi/future.hpp>
#include <jni.h>

extern "C"
{
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_AnyObject_property(JNIEnv* env, jobject jobj, jlong pObj, jstring name);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_AnyObject_setProperty(JNIEnv* env, jobject jobj, jlong pObj, jstring name, jobject property);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_AnyObject_asyncCall(JNIEnv* env, jobject jobj, jlong pObj, jstring methodName, jobjectArray args);
  JNIEXPORT jstring JNICALL Java_com_aldebaran_qi_AnyObject_printMetaObject(JNIEnv* env, jobject jobj, jlong pObj);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_AnyObject_destroy(JNIEnv* env, jobject jobj, jlong pObj);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_AnyObject_connect(JNIEnv *env, jobject obj, jlong pObject, jstring method, jobject instance, jstring service, jstring event);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_AnyObject_disconnect(JNIEnv *env, jobject jobj, jlong pObject, jlong subscriberId);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_AnyObject_post(JNIEnv *env, jobject obj, jlong pObject, jstring eventName, jobjectArray args);
  JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_AnyObject_decodeJSON(JNIEnv* env, jclass cls, jstring str);
  JNIEXPORT jstring JNICALL Java_com_aldebaran_qi_AnyObject_encodeJSON(JNIEnv* env, jclass cls, jobject what);
} // ! extern "C"

#endif // !_JAVA_JNI_OBJECT_HPP_
