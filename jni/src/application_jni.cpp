/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#include <cstring>

#include <qi/log.hpp>
#include <qi/applicationsession.hpp>
#include <jni/jnitools.hpp>
#include <jni/application_jni.hpp>

qiLogCategory("qimessaging.jni");

static jlong app = 0;

jlong createApplication(JNIEnv* env, jobjectArray jargs, const boost::function<jlong(int& argc, char**& argv)>& fn)
{
  if (app)
  {
    throwNewException(env, "Tried to create more than one application");
    return 0;
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

  ++argc; // account for the first argument that we push_front()ed
  int cargc = argc;
  char** cargv = new char*[cargc];
  memcpy(cargv, argv, argc * sizeof(*argv));

  app = fn(cargc, cargv);

  for (int i = 0; i < argc; ++i)
    delete[] argv[i];
  delete[] argv;
  delete[] cargv;

  return app;
}

JNIEXPORT jlong JNICALL newApplicationSession(JNIEnv *env, jstring jdefaultUrl, jboolean listen, int &cargc, char** &cargv)
{
  if (jdefaultUrl)
  {
    const char* cdefurl = env->GetStringUTFChars(jdefaultUrl, NULL);
    std::string defaultUrl(cdefurl);
    env->ReleaseStringUTFChars(jdefaultUrl, cdefurl);

    return (jlong)new qi::ApplicationSession(cargc, cargv, 0, defaultUrl);
  }
  else
    return (jlong)new qi::ApplicationSession(cargc, cargv);
}

JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Application_qiApplicationCreate(JNIEnv *env, jobject QI_UNUSED(obj), jobjectArray jargs, jstring jdefaultUrl, jboolean listen)
{
  return createApplication(env, jargs, boost::function<jlong(int&, char**&)>([=](int &cargc, char** &cargv)
  {
    return newApplicationSession(env, jdefaultUrl, listen, cargc, cargv);
  }));
}

JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Application_qiApplicationGetSession(JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pApplication)
{
  qi::ApplicationSession* app = reinterpret_cast<qi::ApplicationSession*>(pApplication);

  return (jlong)app->session().get();
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_Application_qiApplicationDestroy(JNIEnv *env, jobject QI_UNUSED(obj), jlong pApplication)
{
  qi::Application* app = reinterpret_cast<qi::Application *>(pApplication);

  delete app;
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_Application_qiApplicationStart(JNIEnv *env, jobject QI_UNUSED(obj), jlong pApplication)
{
  qi::ApplicationSession* app = reinterpret_cast<qi::ApplicationSession*>(pApplication);

  try
  {
    app->startSession();
  }
  catch (std::exception& e)
  {
    throwNewException(env, e.what());
  }
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_Application_qiApplicationRun(JNIEnv *env, jobject QI_UNUSED(obj), jlong pApplication)
{
  qi::ApplicationSession* app = reinterpret_cast<qi::ApplicationSession*>(pApplication);

  try
  {
    app->run();
  }
  catch (std::exception& e)
  {
    throwNewException(env, e.what());
  }
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_Application_qiApplicationStop(JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pApplication)
{
  qi::ApplicationSession* app = reinterpret_cast<qi::ApplicationSession*>(pApplication);

  app->stop();
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_Application_setLogCategory(JNIEnv *env, jclass cls, jstring category, jlong verbosity)
{
  ::qi::log::addFilter(qi::jni::toString(category), (qi::LogLevel)verbosity, 0);
}
