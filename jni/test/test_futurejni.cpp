#include <gtest/gtest.h>
#include <jni.h>
#include <jni/jnitools.hpp>
#include <jni/future_jni.hpp>
#include <jni/jobjectconverter.hpp>
#include <jni/object.hpp>

#include <dirent.h>
#include <sys/types.h>

#include "test_common.hpp"

using namespace qi;
using namespace qi::jni;

template <typename T>
ScopedJObject<jobject> toJavaThenWait(const Future<T>& future, bool waitForValue)
{
    // precondition
    QI_ASSERT_TRUE(future.isValid());

    // creates the future object
    auto fut = toJavaFuture(test::environment->jniEnv, future);
    EXPECT_NE(nullptr, fut.value);

    if(!waitForValue)
        return fut;

    // checks the getValue does not fail
    const auto getMethod = test::environment->jniEnv->GetMethodID(cls_future, "getValue", "()Ljava/lang/Object;");
    QI_ASSERT_NOT_NULL(getMethod);

    auto javaValue = scopeJObject(test::environment->jniEnv->CallObjectMethod(fut.value, getMethod));
    handlePendingException(*test::environment->jniEnv);

    QI_ASSERT_NOT_NULL(javaValue.value);
    return fut;
}

template <typename T>
qi::Future<qi::AnyValue> goingAndComingBetweenFutureCAndFutureJava(const Future<T>& future, bool waitForValue = true)
{
    const auto javaFuture = toJavaThenWait<T>(future, waitForValue);
    return toCppFuture(test::environment->jniEnv, javaFuture.value);
}

TEST(FutureJNITest, HasSameIntValueAfterCxxJavaConversion)
{
    Future<int> input(42);
    const auto fut = goingAndComingBetweenFutureCAndFutureJava(input);
    ASSERT_EQ(fut.value().toInt(), 42);
}

TEST(FutureJNITest, HasSameFloatValueAfterCxxJavaConversion)
{
    const auto fut = goingAndComingBetweenFutureCAndFutureJava(futurize(2.5f));
    ASSERT_EQ(fut.value().toFloat(), 2.5f);
}

TEST(FutureJNITest, HasSameAnyValueValueAfterCxxJavaConversion)
{
    const auto fut = goingAndComingBetweenFutureCAndFutureJava(futurize(AnyValue::from(42)));
    ASSERT_EQ(fut.value().toInt(), 42);
}

TEST(FutureJNITest, StaysNullAndValidAfterCxxJavaConversion)
{
    const auto fut = goingAndComingBetweenFutureCAndFutureJava(futurize());
    ASSERT_TRUE(fut.value().isValid());
    ASSERT_FALSE(fut.value().isValue());
}

TEST(FutureJNITest, HasSameFlattenedValueAfterCxxJavaConversion)
{
    Future<int> fut(42);
    auto fut1 = futurize(fut);
    auto fut2 = futurize(fut1);
    auto fut3 = futurize(fut2);
    const auto futJava = goingAndComingBetweenFutureCAndFutureJava(fut3);
    ASSERT_EQ(futJava.value().toInt(), 42);
}

TEST(FutureJNITest, HasSameVectorValueAfterCxxJavaConversion)
{
    const std::vector<int> vec{10, 2, 3};
    const auto fut = goingAndComingBetweenFutureCAndFutureJava(futurize(vec));
    const auto res = fut.value().to<std::vector<int>>();
    ASSERT_EQ(res.size(), 3);
    ASSERT_EQ(res[0], 10);
    ASSERT_EQ(res[1], 2);
    ASSERT_EQ(res[2], 3);
}

TEST(FutureJNITest, HasSameStringValueAfterCxxJavaConversion)
{
    const auto fut = goingAndComingBetweenFutureCAndFutureJava(Future<std::string>("toto"));
    ASSERT_EQ(fut.value().to<std::string>(), "toto");
}

TEST(FutureJNITest, StaysCanceledAfterCxxJavaConversion)
{
    Promise<void> promise([](Promise<void>& prom){ prom.setCanceled(); });
    auto input = promise.future();
    input.cancel();
    const auto fut = goingAndComingBetweenFutureCAndFutureJava(input, false);
    fut.wait();
    ASSERT_TRUE(fut.isCanceled());
}

TEST(FutureJNITest, StaysRunningAfterCxxJavaConversion)
{
    Promise<void> prom;
    auto input = prom.future();
    ASSERT_TRUE(input.isRunning());
    const auto fut = goingAndComingBetweenFutureCAndFutureJava(input, false);
    ASSERT_TRUE(fut.isRunning());
    prom.setValue(nullptr);
}
