/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
*/

#include <cstring>

#include <qi/log.hpp>
#include <qi/application.hpp>
#include <jnitools.hpp>
#include "application_jni.hpp"

qiLogCategory("qimessaging.jni");

static qi::Application* app = 0;
jlong Java_com_aldebaran_qimessaging_Application_qiApplicationCreate(JNIEnv *env, jclass QI_UNUSED(jobj), jobjectArray jargs)
{
  if (app)
  {
    qiLogVerbose() << "Returning already created application.";
    return (jlong)app;
  }

  int argc = env->GetArrayLength(jargs);
  char **argv = new char*[argc + 1];

  // can we do something about this?
  argv[0] = new char[5];
  memcpy(argv[0], "java", 5);

  for (int i = 0; i < argc; ++i)
  {
    jstring jarg = (jstring)env->GetObjectArrayElement(jargs, i);
    jsize arglen = env->GetStringUTFLength(jarg);
    argv[i+1] = new char[arglen+1];
    const char* argchars = env->GetStringUTFChars(jarg, NULL);
    memcpy(argv[i+1], argchars, arglen);
    argv[i+1][arglen] = '\0';
    env->ReleaseStringUTFChars(jarg, argchars);
  }

  int cargc = argc;
  char** cargv = new char*[cargc];
  memcpy(cargv, argv, argc * sizeof(*argv));

  app = new qi::Application(cargc, cargv);

  for (int i = 0; i < argc+1; ++i)
    delete[] argv[i];
  delete[] argv;
  delete[] cargv;

  return (jlong)app;
}

void Java_com_aldebaran_qimessaging_Application_qiApplicationDestroy(JNIEnv *,jclass, jlong pApplication)
{
  //qi::Application* app = reinterpret_cast<qi::Application *>(pApplication);

  //delete app;
}

void Java_com_aldebaran_qimessaging_Application_qiApplicationRun(JNIEnv *, jclass, jlong pApplication)
{
  qi::Application* app = reinterpret_cast<qi::Application *>(pApplication);

  app->run();
}

void Java_com_aldebaran_qimessaging_Application_qiApplicationStop(JNIEnv *, jclass, jlong pApplication)
{
  qi::Application* app = reinterpret_cast<qi::Application *>(pApplication);

  qiLogInfo("qimessaging.jni") << "Stopping qi::Application...";
  app->stop();
}

void Java_com_aldebaran_qimessaging_Application_setLogCategory(JNIEnv *env, jclass cls, jstring category, jlong verbosity)
{
  ::qi::log::addFilter(qi::jni::toString(category), (qi::LogLevel)verbosity, 0);
}
