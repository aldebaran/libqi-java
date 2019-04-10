/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#ifndef _JAVA_JNI_SESSION_HPP_
#define _JAVA_JNI_SESSION_HPP_

#include <jni.h>
#include <qi/anyvalue.hpp>
#include <string>
#include <map>

extern "C"
{
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Session_qiSessionCreate(JNIEnv *env, jobject obj);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Session_qiSessionDestroy(JNIEnv *env, jobject obj, jlong pSession);
  JNIEXPORT jboolean JNICALL Java_com_aldebaran_qi_Session_qiSessionIsConnected(JNIEnv *env, jobject obj, jlong pSession);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Session_qiSessionConnect(JNIEnv *env, jobject obj, jlong pSession, jstring jurl);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Session_qiSessionClose(JNIEnv *env, jobject obj, jlong pSession);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Session_service(JNIEnv* env, jobject obj, jlong pSession, jstring jname);
  JNIEXPORT jint JNICALL Java_com_aldebaran_qi_Session_registerService(JNIEnv *env, jobject obj, jlong pSession, jstring name, jobject object);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Session_unregisterService(JNIEnv *env, jobject obj, jlong pSession, jint serviceId);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Session_onDisconnected(JNIEnv *env, jobject obj, jlong pSession, jstring callbackName, jobject objectInstance);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Session_addConnectionListener(JNIEnv *env, jobject obj, jlong pSession, jobject listener);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Session_setClientAuthenticatorFactory(JNIEnv *env, jobject obj, jlong pSession, jobject object);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Session_loadService(JNIEnv *env, jobject obj, jlong pSession, jstring moduleName);
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Session_endpoints(JNIEnv * env, jobject obj, jlong pSession, jobject endpoints);
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Session_waitForService(JNIEnv * env, jobject obj, jlong pointerSession,  jstring jServiceName);
} // !extern "C"

#endif // !_JAVA_JNI_SESSION_HPP_

