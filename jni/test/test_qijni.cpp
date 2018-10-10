#include <gtest/gtest.h>
#include <jni.h>
#include <ka/errorhandling.hpp>
#include <qi/type/dynamicobjectbuilder.hpp>
#include <qi/property.hpp>
#include <jnitools.hpp>
#include <jobjectconverter.hpp>
#include <object.hpp>
#include <objectbuilder.hpp>


qiLogCategory("qimessaging.jni.test");

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
  void SetUp() override
  {
    JavaVMOption options[1];
    JavaVMInitArgs vm_args;

    char classPathDefinition[] = "-Djava.class.path=.";
    options[0].optionString = classPathDefinition;
    memset(&vm_args, 0, sizeof(vm_args));
    vm_args.version = QI_JNI_MIN_VERSION;
    vm_args.nOptions = 1;
    vm_args.options = options;
    assertSuccess(
      [&]{ return JNI_CreateJavaVM(&javaVM, reinterpret_cast<void**>(&jniEnv), &vm_args); },
      "Failed to create the Java virtual machine.");
    assertSuccess(
      [&]{ return javaVM->AttachCurrentThread(reinterpret_cast<void**>(&jniEnv), nullptr); },
      "Failed to attach current thread.");

    // Real Java apps will always call this when loading the library.
    ASSERT_NO_THROW(EXPECT_EQ(QI_JNI_MIN_VERSION, JNI_OnLoad(javaVM, nullptr)));
    ASSERT_NO_THROW(Java_com_aldebaran_qi_EmbeddedTools_initTypeSystem(jniEnv));
  }

  void TearDown() override
  {
    ASSERT_NO_THROW(JNI_OnUnload(javaVM, nullptr));

    jniEnv = nullptr;
    if (const auto localJavaVM = ka::exchange(javaVM, nullptr))
    {
      assertSuccess([&]{ return localJavaVM->DetachCurrentThread(); },
                    "Failed to detach current thread.");
      assertSuccess([&]{ return localJavaVM->DestroyJavaVM(); },
                    "Failed to destroy the Java virtual machine.");
    }
  }

public:
  JavaVM* javaVM = nullptr;
  JNIEnv* jniEnv = nullptr;
};

Environment* environment = nullptr;

namespace
{

template <typename T>
AnyObject makeObjectWithProperty(const std::string& propertyName, Property<T>& property)
{
  DynamicObjectBuilder objectBuilder;
  objectBuilder.advertiseProperty(propertyName, &property);
  return objectBuilder.object();
}

} // anonymous namespace

}}} // namespace jni::test

using namespace qi;
using namespace qi::jni;

int main(int argc, char** argv)
{
  // Avoid making a qi::Application. Real Java apps cannot do it.
  log::addFilter("qimessaging.jni", LogLevel_Debug);
  ::testing::InitGoogleTest(&argc, argv);
  test::environment =
    static_cast<test::Environment*>(::testing::AddGlobalTestEnvironment(new test::Environment));
  return RUN_ALL_TESTS();
}

TEST(QiJNI, setProperty)
{
  const int initialValue = 12;
  const int newValue = 42;
  Property<int> property{initialValue};

  const std::string propertyName = "serendipity";
  auto object = test::makeObjectWithProperty(propertyName, property);
  auto objectPtr = &object;

  JNIAttach attach{test::environment->jniEnv};
  auto futureAddress = Java_com_aldebaran_qi_AnyObject_setProperty(
        test::environment->jniEnv, jobject{},
        reinterpret_cast<jlong>(objectPtr),
        toJstring(propertyName),
        JObject_from_AnyValue(AnyValue{newValue}.asReference()));

  auto future = reinterpret_cast<Future<AnyValue>*>(futureAddress);
  auto status = future->waitFor(MilliSeconds{200});
  ASSERT_EQ(FutureState_FinishedWithValue, status);
  ASSERT_EQ(newValue, object.property<int>(propertyName).value());
}

TEST(QiJNI, futureErrorIfSetPropertyThrows)
{
  using CustomException = std::exception;
  const int initialValue = 12;
  const int newValue = 42;
  Property<int> property{initialValue, Property<int>::Getter{}, [this](int&, const int&)->bool
  {
    throw CustomException{};
  }};

  const std::string propertyName = "serendipity";
  auto object = test::makeObjectWithProperty(propertyName, property);
  auto objectPtr = &object;

  JNIAttach attach{test::environment->jniEnv};
  auto futureAddress = Java_com_aldebaran_qi_AnyObject_setProperty(
        test::environment->jniEnv, jobject{},
        reinterpret_cast<jlong>(objectPtr),
        toJstring(propertyName),
        JObject_from_AnyValue(AnyValue{newValue}.asReference()));

  auto future = reinterpret_cast<Future<AnyValue>*>(futureAddress);
  auto status = future->wait();
  ASSERT_EQ(FutureState_FinishedWithError, status);
  ASSERT_EQ(initialValue, object.property<int>(propertyName).value());
}


TEST(QiJNI, dynamicObjectBuilderAdvertiseMethodVoidVoid)
{
  // Object.notify() is a typical example of function taking and returning nothing.
  const auto javaObjectClassName = "java/lang/Object";
  const auto javaObjectClass = test::environment->jniEnv->FindClass(javaObjectClassName);
  const auto javaObjectConstructor =
    test::environment->jniEnv->GetMethodID(javaObjectClass, "<init>", "()V");
  const auto javaObject = test::environment->jniEnv->NewObject(javaObjectClass, javaObjectConstructor);

  // Let's make a Qi Object calling that method.
  const auto objectBuilderAddress =
    Java_com_aldebaran_qi_DynamicObjectBuilder_create(test::environment->jniEnv, nullptr);
  test::environment->jniEnv->ExceptionClear();
  Java_com_aldebaran_qi_DynamicObjectBuilder_advertiseMethod(
        test::environment->jniEnv, jobject{},
        objectBuilderAddress,
        toJstring("notify::v()"), javaObject, // 'v' stands for "void"
        toJstring(javaObjectClassName),
        toJstring("Whatever"));
  ASSERT_FALSE(test::environment->jniEnv->ExceptionCheck());
}
