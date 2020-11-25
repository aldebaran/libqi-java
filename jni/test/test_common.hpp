#ifndef TEST_COMMON_H
#define TEST_COMMON_H

#include <gtest/gtest.h>
#include <qi/type/dynamicobjectbuilder.hpp>
#include <qi/property.hpp>
#include <jni/jnitools.hpp>
#include <jni.h>
#include <qi/application.hpp>

namespace qi { namespace jni { namespace test
{

namespace
{
  /// Procedure<jint()> Proc
  /// OStreamable O
  template<typename Proc, typename O = const char*>
  void assertSuccess(Proc&& p, O&& errorPrefix = {})
  {
    const auto status = std::forward<Proc>(p)();
    if (status != JNI_OK)
      FAIL() << std::forward<O>(errorPrefix) << "Status: " << errorToString(status);
  }
}

class Environment : public ::testing::Environment
{
  void SetUp() override;
  void TearDown() override;

  // This function reads the command line options to deduce parameters.
  // Currently, the only parameter is java classes path and it is required.
  void readArguments();
public:
  Environment(int argc, char** argv);
  JavaVM* javaVM = nullptr;
  JNIEnv* jniEnv = nullptr;
  int argc;
  char** argv;
  std::vector<std::string> classPaths;
  boost::optional<qi::Application> app;
};

extern Environment* environment;

namespace
{

template <typename T>
qi::AnyObject makeObjectWithProperty(const std::string& propertyName, qi::Property<T>& property)
{
  DynamicObjectBuilder objectBuilder;
  objectBuilder.advertiseProperty(propertyName, &property);
  return objectBuilder.object();
}

} // anonymous namespace

}}} // namespace jni::test

#endif // TEST_COMMON_H
