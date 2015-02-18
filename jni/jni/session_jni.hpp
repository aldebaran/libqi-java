/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
*/

#ifndef _JAVA_JNI_SESSION_HPP_
#define _JAVA_JNI_SESSION_HPP_

#include <jni.h>

extern "C"
{
  JNIEXPORT jlong     Java_com_aldebaran_qi_Session_qiSessionCreate();
  JNIEXPORT void      Java_com_aldebaran_qi_Session_qiSessionDestroy(JNIEnv* QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pSession);
  JNIEXPORT jboolean  Java_com_aldebaran_qi_Session_qiSessionIsConnected(JNIEnv *env, jobject obj, jlong pSession);
  JNIEXPORT jlong     Java_com_aldebaran_qi_Session_qiSessionConnect(JNIEnv *env, jobject obj, jlong pSession, jstring jurl);
  JNIEXPORT void      Java_com_aldebaran_qi_Session_qiSessionClose(JNIEnv *env, jobject obj, jlong pSession);
  JNIEXPORT jobject   Java_com_aldebaran_qi_Session_service(JNIEnv* env, jobject obj, jlong pSession, jstring jname);
  JNIEXPORT jint      Java_com_aldebaran_qi_Session_registerService(JNIEnv *env, jobject obj, jlong pSession, jstring name, jobject object);
  JNIEXPORT void      Java_com_aldebaran_qi_Session_unregisterService(JNIEnv *env, jobject obj, jlong pSession, jint serviceId);
  JNIEXPORT void      Java_com_aldebaran_qi_Session_onDisconnected(JNIEnv *env, jobject obj, jlong pSession, jstring callbackName, jobject objectInstance);

} // !extern "C"

#endif // !_JAVA_JNI_SESSION_HPP_

