/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
*/

#include <qi/log.hpp>
#include <qimessaging/session.hpp>
#include "jnitools.hpp"
#include "servicedirectory_jni.hpp"

/**
 * @brief Java_com_aldebaran_qimessaging_ServiceDirectory_qiTestSDCreate
 * @param env JNI environment, mandatory argument.
 * @param obj JNI environment, mandatory argument.
 * @return a pointer on C++ ServiceDirectory instance
 *
 * This class only exists for tests purpose. That is why addresse is not customisable.
 */
jlong   Java_com_aldebaran_qimessaging_ServiceDirectory_qiTestSDCreate(JNIEnv *env, jobject obj)
{
  qi::Session *sd = new qi::Session();

  qi::Future<void> fut = sd->listenStandalone("tcp://0.0.0.0:0");
  fut.wait();
  if (fut.hasError())
  {
    std::stringstream ss;

    ss << "Cannot get test Service Directory: " << fut.error();
    qiLogError("qimessaging.jni") << ss.str();
    delete sd;
    throwJavaError(env, ss.str().c_str());
    return (jlong) 0;
  }

  return (jlong) sd;
}

void    Java_com_aldebaran_qimessaging_ServiceDirectory_qiTestSDDestroy(jlong pSD)
{
  qi::Session *sd = reinterpret_cast<qi::Session *>(pSD);

  delete sd;
}

jstring Java_com_aldebaran_qimessaging_ServiceDirectory_qiListenUrl(JNIEnv* QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pSD)
{
  qi::Session *sd = reinterpret_cast<qi::Session *>(pSD);

  if (!sd)
  {
    qiLogError("qimessaging.jni")  << "ServiceDirectory doesn't exist.";
    return 0;
  }

  return qi::jni::toJstring(sd->endpoints().at(0).str());
}

void    Java_com_aldebaran_qimessaging_ServiceDirectory_qiTestSDClose(jlong pSD)
{
  qi::Session *sd = reinterpret_cast<qi::Session *>(pSD);

  if (!sd)
  {
    qiLogError("qimessaging.jni") << "Reference to Service Directory is null.";
    return;
  }

  sd->close();
}
