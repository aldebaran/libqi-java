/*
**
** Author(s):
**  - Philippe Daouadi <pdaouadi@aldebaran.com>
**
** Copyright (C) 2015 Aldebaran Robotics
** See COPYING for the license
*/

#include <cstring>

#include <qi/log.hpp>
#include <qi/applicationsession.hpp>
#include <jni/jnitools.hpp>
#include <jni/application_jni.hpp>
#include <jni/rawapplication_jni.hpp>

qiLogCategory("qimessaging.jni");

jlong newApplication(int& cargc, char**& cargv)
{
  return (jlong)new qi::Application(cargc, cargv);
}

JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_RawApplication_qiApplicationCreate(JNIEnv *env, jobject QI_UNUSED(obj), jobjectArray jargs, jstring jdefaultUrl, jboolean listen)
{
  return createApplication(env, jargs, boost::bind(newApplication, _1, _2));
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_RawApplication_qiApplicationDestroy(JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pApplication)
{
  qi::Application* app = reinterpret_cast<qi::Application *>(pApplication);

  delete app;
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_RawApplication_qiApplicationRun(JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pApplication)
{
  qi::Application* app = reinterpret_cast<qi::Application*>(pApplication);

  app->run();
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_RawApplication_qiApplicationStop(JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pApplication)
{
  qi::Application* app = reinterpret_cast<qi::Application*>(pApplication);

  app->stop();
}
