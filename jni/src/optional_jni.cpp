#include <jnitools.hpp>
#include <qi/log.hpp>
#include <optional_jni.hpp>

JNIOptional::JNIOptional()
{
  javaVirtualMachine->GetEnv((void**) &_env, QI_JNI_MIN_VERSION);
  jmethodID mid = _env->GetMethodID(cls_optional, "<init>", "()V");
  if (!mid)
  {
    const auto msg = "JNIOptional::JNIOptional: Cannot call constructor";
    qiLogFatal("qimessaging.jni") << msg;
    throw std::runtime_error(msg);
  }

  _obj = _env->NewObject(cls_optional, mid);
  
  qi::jni::handlePendingException(*_env);
}

JNIOptional::JNIOptional(OptionalInitFromInstance, jobject obj)
{
  javaVirtualMachine->GetEnv((void**) &_env, QI_JNI_MIN_VERSION);
  _obj = _env->NewLocalRef(obj);
}

JNIOptional::JNIOptional(OptionalInitFromValue, jobject value)
{
  javaVirtualMachine->GetEnv((void**) &_env, QI_JNI_MIN_VERSION);

  jmethodID mid = _env->GetMethodID(cls_optional, "<init>", "(Ljava/lang/Object;)V");
  if (!mid)
  {
    const auto msg = "JNIOptional::JNIOptional: Cannot call constructor";
    qiLogFatal("qimessaging.jni") << msg;
    throw std::runtime_error(msg);
  }

  _obj = _env->NewObject(cls_optional, mid, value);

  qi::jni::handlePendingException(*_env);
}

JNIOptional::~JNIOptional() {
  qi::jni::releaseObject(_obj);
  
}

jobject JNIOptional::value() const
{
  jmethodID mid = _env->GetMethodID(cls_optional, "get", "()Ljava/lang/Object;");

  if (!mid)
  {
    const auto msg = "JNIOptional : Cannot call method get()Ljava/lang/Object;";
    qiLogFatal("qimessaging.jni") << msg;
    throw std::runtime_error(msg);
  }

  auto handle = ka::scoped([&](){
    qi::jni::handlePendingException(*_env);
  });
  
  return _env->CallObjectMethod(_obj, mid);
}

bool JNIOptional::hasValue() const
{
  jmethodID mid = _env->GetMethodID(cls_optional, "isPresent", "()Z");

  if (!mid)
  {
    const auto msg = "JNIOptional : Cannot call method isPresent()Z";
    qiLogFatal("qimessaging.jni") << msg;
    throw std::runtime_error(msg);
  }

  auto handle = ka::scoped([&](){
    qi::jni::handlePendingException(*_env);
  });

  return _env->CallBooleanMethod(_obj, mid);
}

jobject JNIOptional::object() const
{
  return _env->NewLocalRef(_obj);
}
