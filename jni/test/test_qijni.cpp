#include <gtest/gtest.h>
#include <boost/algorithm/string/replace.hpp>
#include <ka/errorhandling.hpp>
#include <jni/jobjectconverter.hpp>
#include <jni/object.hpp>
#include <jni/objectbuilder.hpp>

#include "test_common.hpp"

qiLogCategory("qimessaging.jni.test");

using namespace qi;
using namespace qi::jni;

int main(int argc, char** argv)
{
  // Avoid making a qi::Application. Real Java apps cannot do it.
  log::addFilter("qimessaging.jni", LogLevel_Debug);
  ::testing::InitGoogleTest(&argc, argv);
  test::environment =
    static_cast<test::Environment*>(::testing::AddGlobalTestEnvironment(new test::Environment(argc, argv)));
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

TEST(QiJNI, propertyOfAnyObjectAcceptsNull)
{
  Property<int> whatev(42);
  const auto initialValue = test::makeObjectWithProperty("tomtom", whatev);
  const auto newValue = AnyObject();
  Property<AnyObject> property{AutoAnyReference(initialValue)};

  const std::string propertyName = "nana";
  auto object = test::makeObjectWithProperty(propertyName, property);
  auto objectPtr = &object;

  JNIAttach attach{test::environment->jniEnv};
  auto futureAddress = Java_com_aldebaran_qi_AnyObject_setProperty(
        test::environment->jniEnv, jobject{},
        reinterpret_cast<jlong>(objectPtr),
        toJstring(propertyName),
        nullptr);

  auto future = reinterpret_cast<Future<AnyValue>*>(futureAddress);
  switch(const auto status = future->wait(MilliSeconds{200}))
  {
    case FutureState_FinishedWithValue:
      break;
    case FutureState_FinishedWithError:
      FAIL() << future->error();
      break;
    default:
      FAIL() << status;
  }
  ASSERT_FALSE(object.property<AnyObject>(propertyName).value());
  ASSERT_FALSE(test::environment->jniEnv->ExceptionCheck());
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

TEST(QiJNI, className)
{
  const std::string className("java/lang/NullPointerException");
  const auto clazz = test::environment->jniEnv->FindClass(className.c_str());
  const auto actual = name(clazz);
  ASSERT_TRUE(actual);
  EXPECT_EQ(boost::replace_all_copy(className, "/", "."), *actual);
}

TEST(QiJNITypeConversion, NullJavaObjectToQiObjectConvertsToNullObject)
{
  const auto jobj = ka::scoped(test::environment->jniEnv->NewGlobalRef(nullptr), [](jobject obj) {
    test::environment->jniEnv->DeleteGlobalRef(obj);
  });
  const auto ref = ka::scoped(AnyValue_from_JObject(jobj.value, typeOf<AnyObject>()->signature()),
                              [&](std::pair<AnyReference, bool> p) {
                                if (p.second)
                                  p.first.destroy();
                              });

  // qi.Objects actually are of type Dynamic in the type system, not of type Object.
  ASSERT_EQ(TypeKind_Dynamic, ref.value.first.kind());
  AnyObject obj;
  EXPECT_NO_THROW(obj = ref.value.first.to<AnyObject>());
  EXPECT_FALSE(obj.isValid());
}

namespace
{
  template<typename ObjectType>
  auto validate(ObjectType obj)
    // TODO: Remove the trailing return type when we can switch to C++14.
    -> decltype(ka::scoped(obj, &qi::jni::releaseObject))
  {
    auto& env = *test::environment->jniEnv;
    EXPECT_EQ(JNI_FALSE, env.IsSameObject(obj, nullptr));
    return ka::scoped(obj, &qi::jni::releaseObject);
  }
} // anonymous namespace

TEST(QiJNITypeConversion, EmptyCppMapConvertsToEmptyJavaMap)
{
  auto& env = *test::environment->jniEnv;
  auto javaMap = validate(qi::jni::toJavaStringObjectMap(env, {}));
  EXPECT_EQ(JNI_TRUE, qi::jni::Call<jboolean>::invoke(&env, javaMap.value, "isEmpty", "()Z"));
}

TEST(QiJNITypeConversion, EmptyJavaMapConversToEmptyCppMap)
{
  auto& env = *test::environment->jniEnv;
  auto javaMap = qi::jni::construct(&env, "java/util/HashMap", "()V");
  auto cppMap = qi::jni::toCppStringAnyValueMap(env, javaMap);
  EXPECT_TRUE(cppMap.empty());
}

TEST(QiJNITypeConversion, CppMapConvertsToJavaMap)
{
  using namespace qi;
  auto& env = *test::environment->jniEnv;

  const auto javaMap = validate(
    jni::toJavaStringObjectMap(env,
                                   { { "first_key", AnyValue::from(std::string("first_value")) },
                                     { "second_key", AnyValue::from(2) } }));
  EXPECT_EQ(2, jni::Call<jint>::invoke(&env, javaMap.value, "size", "()I"));

  const auto entrySet = validate(
    jni::Call<jobject>::invoke(&env, javaMap.value, "entrySet", "()Ljava/util/Set;"));
  const auto iterator = validate(jni::Call<jobject>::invoke(&env, entrySet.value, "iterator",
                                                            "()Ljava/util/Iterator;"));

  ASSERT_EQ(JNI_TRUE, jni::Call<jboolean>::invoke(&env, iterator.value, "hasNext", "()Z"));
  const auto firstEntry = validate(
    jni::Call<jobject>::invoke(&env, iterator.value, "next", "()Ljava/lang/Object;"));
  const auto firstKey = validate(reinterpret_cast<jstring>(
    jni::Call<jobject>::invoke(&env, firstEntry.value, "getKey", "()Ljava/lang/Object;")));
  const auto firstValue = validate(reinterpret_cast<jstring>(
    jni::Call<jobject>::invoke(&env, firstEntry.value, "getValue", "()Ljava/lang/Object;")));
  EXPECT_EQ("first_key", jni::toString(firstKey.value));
  EXPECT_EQ("first_value", jni::toString(firstValue.value));


  ASSERT_EQ(JNI_TRUE, jni::Call<jboolean>::invoke(&env, iterator.value, "hasNext", "()Z"));
  const auto secondEntry = validate(
    jni::Call<jobject>::invoke(&env, iterator.value, "next", "()Ljava/lang/Object;"));
  const auto secondKey = validate(reinterpret_cast<jstring>(
    jni::Call<jobject>::invoke(&env, secondEntry.value, "getKey", "()Ljava/lang/Object;")));
  const auto secondValue = validate(
    jni::Call<jobject>::invoke(&env, secondEntry.value, "getValue", "()Ljava/lang/Object;"));
  EXPECT_EQ("second_key", jni::toString(secondKey.value));

  const auto mid = env.GetMethodID(cls_integer, "intValue", "()I");
  EXPECT_EQ(2, env.CallIntMethod(secondValue.value, mid));
}

TEST(QiJNITypeConversion, JavaMapConvertsToCppMap)
{
  using namespace qi;
  auto& env = *test::environment->jniEnv;

  const auto javaMap = validate(jni::construct(&env, "java/util/HashMap", "()V"));

  const auto firstKey = validate(jni::toJstring("Java_first_key"));
  const auto firstValue = validate(jni::construct(&env, "java/lang/Integer", "(I)V", 42));
  ka::scoped(
    jni::Call<jobject>::invoke(&env, javaMap.value, "put",
                                   "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                                   firstKey.value, firstValue.value),
    &jni::releaseObject);

  const auto secondKey = validate(jni::toJstring("Java_second_key"));
  const auto secondValue = validate(jni::toJstring("Java_second_value"));
  ka::scoped(
    jni::Call<jobject>::invoke(&env, javaMap.value, "put",
                                   "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                                   secondKey.value, secondValue.value),
    &jni::releaseObject);

  const auto map = jni::toCppStringAnyValueMap(env, javaMap.value);
  const auto expected = std::map<std::string, qi::AnyValue>{
    {"Java_first_key", AnyValue::from(42)},
    {"Java_second_key", AnyValue::from(std::string{"Java_second_value"})}
  };

  EXPECT_EQ(expected, map);
}
