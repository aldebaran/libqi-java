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
jlong Java_com_aldebaran_qimessaging_Application_qiApplicationCreate(JNIEnv *env, jclass QI_UNUSED(jobj))
{
  if (app)
  {
    qiLogVerbose() << "Returning already created application.";
    return (jlong)app;
  }
  // Emulate empty command line arguments.
  int argc = 1;
  char **argv = new char*[2];

  // Fill first argument with jvm name. FIXME : seriously....
  argv[0] = new char[10];
  ::strcpy(argv[0], "java");

  // Call qi_application_create
  app = new qi::Application(argc, argv);

  delete[] argv[0];
  delete[] argv;
  return (jlong)app;
}

void Java_com_aldebaran_qimessaging_Application_qiApplicationDestroy(JNIEnv *,jlong pApplication)
{
  //qi::Application* app = reinterpret_cast<qi::Application *>(pApplication);

  //delete app;
}

void Java_com_aldebaran_qimessaging_Application_qiApplicationRun(JNIEnv *, jlong pApplication)
{
  qi::Application* app = reinterpret_cast<qi::Application *>(pApplication);

  app->run();
}

void Java_com_aldebaran_qimessaging_Application_qiApplicationStop(JNIEnv *, jlong pApplication)
{
  qi::Application* app = reinterpret_cast<qi::Application *>(pApplication);

  qiLogInfo("qimessaging.jni") << "Stopping qi::Application...";
  app->stop();
}
