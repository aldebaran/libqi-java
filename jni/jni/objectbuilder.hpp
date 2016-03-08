/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#ifndef _JAVA_JNI_OBJECTBUILDER_HPP_
#define _JAVA_JNI_OBJECTBUILDER_HPP_

#include <jni.h>

extern "C"
{
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_DynamicObjectBuilder_create(JNIEnv *env, jobject obj);
  JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_DynamicObjectBuilder_object(JNIEnv *env, jobject jobj, jlong pObjectBuilder);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_DynamicObjectBuilder_destroy(JNIEnv *env, jobject jobj, jlong pObjectBuilder);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_DynamicObjectBuilder_advertiseMethod(JNIEnv *env, jobject obj, jlong pObjectBuilder, jstring method, jobject instance, jstring service, jstring desc);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_DynamicObjectBuilder_advertiseSignal(JNIEnv *env, jobject obj, jlong pObjectBuilder, jstring eventSignature);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_DynamicObjectBuilder_advertiseProperty(JNIEnv *env, jobject obj, jlong pObjectBuilder, jstring name, jclass propertyBase);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_DynamicObjectBuilder_advertiseThreadSafeness(JNIEnv *env, jobject obj, jlong pObjectBuilder, jboolean isThreadSafe);


} // !extern "C"

#endif // !_JAVA_JNI_OBJECTBUILDER_HPP_

