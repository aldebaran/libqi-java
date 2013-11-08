/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
*/

#ifndef _JAVA_JNI_OBJECTBUILDER_HPP_
#define _JAVA_JNI_OBJECTBUILDER_HPP_

#include <jni.h>

extern "C"
{

  JNIEXPORT jlong   Java_com_aldebaran_qimessaging_DynamicObjectBuilder_create();
  JNIEXPORT jobject Java_com_aldebaran_qimessaging_DynamicObjectBuilder_object(JNIEnv *env, jobject jobj, jlong pObjectBuilder);
  JNIEXPORT void    Java_com_aldebaran_qimessaging_DynamicObjectBuilder_destroy(JNIEnv *env, jobject jobj, jlong pObjectBuilder);
  JNIEXPORT jlong   Java_com_aldebaran_qimessaging_DynamicObjectBuilder_advertiseMethod(JNIEnv *env, jobject obj, jlong pObjectBuilder, jstring method, jobject instance, jstring service, jstring desc);
  JNIEXPORT jlong   Java_com_aldebaran_qimessaging_DynamicObjectBuilder_advertiseSignal(JNIEnv *env, jobject obj, jlong pObjectBuilder, jstring eventSignature);
  JNIEXPORT jlong   Java_com_aldebaran_qimessaging_DynamicObjectBuilder_advertiseProperty(JNIEnv *env, jobject obj, jlong pObjectBuilder, jstring name, jclass propertyBase);
  JNIEXPORT jlong   Java_com_aldebaran_qimessaging_DynamicObjectBuilder_advertiseThreadSafeness(JNIEnv *env, jobject obj, jlong pObjectBuilder, jboolean isThreadSafe);


} // !extern "C"

#endif // !_JAVA_JNI_OBJECTBUILDER_HPP_

