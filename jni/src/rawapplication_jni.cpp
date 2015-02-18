/*
**
** Author(s):
**  - Philippe Daouadi <pdaouadi@aldebaran.com>
**
** Copyright (C) 2015 Aldebaran Robotics
*/

#include <cstring>

#include <qi/log.hpp>
#include <qi/applicationsession.hpp>
#include <jnitools.hpp>
#include "application_jni.hpp"
#include "rawapplication_jni.hpp"

qiLogCategory("qimessaging.jni");

qi::Application* newApplication(int& cargc, char**& cargv)
{
  return new qi::Application(cargc, cargv);
}

jlong Java_com_aldebaran_qi_RawApplication_qiApplicationCreate(JNIEnv *env, jclass QI_UNUSED(jobj), jobjectArray jargs, jstring jdefaultUrl, jboolean listen)
{
  return createApplication(env, jargs, boost::bind(newApplication, _1, _2));
}

jlong Java_com_aldebaran_qi_RawApplication_qiApplicationGetSession(JNIEnv *,jclass, jlong pApplication)
{
  qi::ApplicationSession* app = reinterpret_cast<qi::ApplicationSession*>(pApplication);

  return (jlong)app->session().get();
}

void Java_com_aldebaran_qi_RawApplication_qiApplicationDestroy(JNIEnv *,jclass, jlong pApplication)
{
  //qi::Application* app = reinterpret_cast<qi::Application *>(pApplication);

  //delete app;
}

void Java_com_aldebaran_qi_RawApplication_qiApplicationRun(JNIEnv *, jclass, jlong pApplication)
{
  qi::Application* app = reinterpret_cast<qi::Application*>(pApplication);

  app->run();
}

void Java_com_aldebaran_qi_RawApplication_qiApplicationStop(JNIEnv *, jclass, jlong pApplication)
{
  qi::Application* app = reinterpret_cast<qi::Application*>(pApplication);

  app->stop();
}
