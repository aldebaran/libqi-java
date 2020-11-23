/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#include <qi/anyvalue.hpp>

#include <jni/jnitools.hpp>
#include <jni/futurehandler.hpp>
#include <jni/future_jni.hpp>
#include <jni/callbridge.hpp>

qiLogCategory("qimessaging.java");

// *************
// *** TOOLS ***
// *************
qi::jni::ScopedJObject<jobject> toJavaFuture(JNIEnv *env, qi::Future<qi::AnyValue> future)
{
    QI_ASSERT_TRUE(future.isValid());
    const auto futurePtr = new qi::Future<qi::AnyValue>(future);
    const auto constructor = env->GetMethodID(cls_future,"<init>","(J)V");
    return qi::jni::scopeJObject(env->NewObject(cls_future, constructor, reinterpret_cast<jlong>(futurePtr)));
}

qi::jni::ScopedJObject<jobject> toJavaFuture(JNIEnv *env, qi::Future<void> future)
{
    return toJavaFuture(env, future.andThen([](void*){ return qi::AnyValue::makeVoid(); }));
}

/**
 * @brief extractValue Extract from AnyValue the embedded value and convert it to Java object
 * @param env JNI environment
 * @param value AnyValue that contains the value
 * @return The extracted/converted Java object
 */
static jobject extractValue(JNIEnv *env, const qi::AnyValue& value)
{
    try
    {
        qi::AnyReference arRes = value.asReference();
        qi::TypeInterface* typeInterface = arRes.type();

        if(typeInterface == nullptr)
        {
            return nullptr;
        }

        if(typeInterface->info() == qi::typeOf<qi::AnyValue>()->info())
        {
            //If the AnyReference contains an AnyValue,
            //Then we get the value from this AnyValue
            return extractValue(env, *reinterpret_cast<qi::AnyValue*>(arRes.rawValue()));
        }

        // FIXME:
        // We cannot use `qi::AnyReference::to<jobject>()` because it would return a jobject that
        // was already released:
        // The `to` method converts the AnyReference into a temporary AnyReference to jobject,
        // copies it in a local jobject (which is a pointer), destroys the temporary (which calls
        // `JNIEnv::DestroyGlobalRef`) and returns the copy of the jobject, which then references a
        // object that was released. Instead we call `AnyReference::convert` directly a use the
        // jobject before the result of the `convert` method is destroyed.
        const auto converted = arRes.convert(qi::typeOf<jobject>());

        //If the converted value doesn't have a valid type, trying to obtain its rawValue will do a SIGSEGV,
        // because the method rawValue() refers to a non initialized value in this case
        if (!converted->isValid())
        {
          //Not valid, return "null" object to Java
          return nullptr;
        }

        //The converted value is valid
        const auto result = *reinterpret_cast<jobject*>(converted->rawValue());
        return env->NewLocalRef(result);
    }
    catch (const std::exception &e)
    {
        // unexpected error => java RuntimeException
        throwNewRuntimeException(env, e.what());
        return nullptr;
    }
    catch (...)
    {
        // unexpected error => java RuntimeException
        throwNewRuntimeException(env, "Unexpected exception while extract the value!");
        return nullptr;
    }
}

/**
 * @brief obtainValue Obtain value of a future and convert it in Java object
 * @param env JNI evironment
 * @param future Future where get the value
 * @param msecs Time out
 * @return Extracted and converted value
 */
static jobject obtainValue(JNIEnv *env, qi::Future<qi::AnyValue>* future, jint msecs)
{
    try
    {
        int qiMsecs = msecs == -1 ? qi::FutureTimeout_Infinite : msecs;
        auto val = future->value(qiMsecs);
        return extractValue(env, val);
    }
    catch (const qi::FutureException &e)
    {
        switch (e.state())
        {
        case qi::FutureException::ExceptionState_FutureTimeout:
            throwNewTimeoutException(env, "native future timeout");
            break;
        case qi::FutureException::ExceptionState_FutureCanceled:
            // libqi uses "canceled"/"cancelation" while java uses "cancelled"/"cancellation"...
            throwNewCancellationException(env, "native future cancelled");
            break;
        case qi::FutureException::ExceptionState_FutureUserError:
            throwNewExecutionException(env, createNewQiException(env, e.what()));
            break;
        default:
            // unexpected error => java RuntimeException
            throwNewRuntimeException(env, e.what());
        }

        return nullptr;
    }
    catch (const std::exception &e)
    {
        // unexpected error => java RuntimeException
        throwNewRuntimeException(env, e.what());
        return nullptr;
    }
    catch (...)
    {
        // unexpected error => java RuntimeException
        throwNewRuntimeException(env, "Unexpected exception while extract the value!");
        return nullptr;
    }
}

/**
 * @brief callFunctionExecute Call 'execute' method from com.aldebaran.qi.Function
 * @param env JNI environment
 * @param function Funtion instance
 * @param argument Argument give to function
 * @return function.execute(argument)
 */
static jobject callFunctionExecute(JNIEnv *env, jobject function, jobject argument)
{
    static const char *method = "execute";
    static const char *methodSig = "(Ljava/lang/Object;)Ljava/lang/Object;";
    jobject answer = qi::jni::Call<jobject>::invoke(env, function, method, methodSig, argument);

    //Check if exception happened on Java side
    qi::jni::handlePendingException(*env);

    return answer;
}

/**
 * @brief callConsumerConsume Call 'consume' method from com.aldebaran.qi.Consumer
 * @param env JNI environment
 * @param function Funtion instance
 * @param argument Argument give to consumer
 */
static void callConsumerConsume(JNIEnv *env, jobject function, jobject argument)
{
    static const char *method = "consume";
    static const char *methodSig = "(Ljava/lang/Object;)V";
    qi::jni::Call<void>::invoke(env, function, method, methodSig, argument);

    //Check if exception happened on Java side
    qi::jni::handlePendingException(*env);
}

/**
 * @brief futureOfNull Future that carry a null value
 * @return Future that carry a null value
 */
static jobject futureOfNull(JNIEnv *env)
{
    static const char *method = "of";
    static const char *methodSig = "(Ljava/lang/Object;)Lcom/aldebaran/qi/Future;";
    jmethodID methodOf = env->GetStaticMethodID(cls_future,method,methodSig);
    return env->CallStaticObjectMethod(cls_future, methodOf, nullptr);
}

qi::Future<qi::AnyValue> toCppFuture(JNIEnv *env, jobject future)
{
    if (env->IsSameObject(future, NULL))
    {
        future = futureOfNull(env);
    }

    // Obtain the C++ pointer embed in Future Java
    jlong pointer = env->GetLongField(future, field_future_pointer);

    //Check if exception happened on Java side
    qi::jni::handlePendingException(*env);

    //Obtain the C++ Future linked to the pointer
    return *reinterpret_cast<qi::Future<qi::AnyValue>*>(pointer);
}

/**
 * @brief futureFromPointer Get future instance that lies behind given pointer address
 * @param pointer Pointer address
 * @return The future instance
 */
static qi::Future<qi::AnyValue>* futureFromPointer(jlong pointer)
{
    return reinterpret_cast<qi::Future<qi::AnyValue>*>(pointer);
}

// ****************
// *** Functors ***
// ****************

struct CallbackFunctor
{
    qi::jni::SharedGlobalRef _argFuture;
    qi::jni::SharedGlobalRef _callback;
    void operator()(const qi::Future<qi::AnyValue> &) const
    {
        // we stored the jobject future, so we ignore the parameter

        jobject argFuture = _argFuture.get();
        jobject callback = _callback.get();

        qi::jni::JNIAttach attach;
        JNIEnv *env = attach.get();

        static const char *method = "onFinished";
        static const char *methodSig = "(Lcom/aldebaran/qi/Future;)V";
        qi::jni::Call<void>::invoke(env, callback, method, methodSig, argFuture);
        if (env->ExceptionCheck() == JNI_TRUE)
        {
            qiLogError() << "Exception when calling Future.Callback.onFinished(…) from JNI";
            env->ExceptionDescribe();
            // the callback threw an exception, it must have no impact on the caller
            env->ExceptionClear();
        }
    }
};

/**
 * @brief The FunctionFunctor struct: P->R
 */
struct FunctionFunctor
{
    qi::jni::SharedGlobalRef _function;
    /**
     * @brief FunctionFunctor::operator () Called by andThen
     * @param res Parent future result
     * @return  Function result
     */
    qi::AnyValue operator()(const qi::AnyValue &res) const
    {
        // Call Java: Function.execute(P)->R
        jobject function = _function.get();

        qi::jni::JNIAttach attach;
        JNIEnv *env = attach.get();

        jobject result = extractValue(env, res);
        jobject answer = callFunctionExecute(env, function, result);

        //Convert answer to AnyValue
        return qi::AnyValue::from<jobject>(answer);
    }
};
/**
 * @brief The FunctionFunctorVoid struct: P->void
 */
struct FunctionFunctorVoid
{
    qi::jni::SharedGlobalRef _function;
    /**
     * @brief FunctionFunctorVoid::operator () Called by andThenVoid
     * @param res Parent future result
     */
    qi::AnyValue operator()(const qi::AnyValue & res) const
    {
        // Call Java: Consumer.consume(P)->void
        jobject function = _function.get();

        qi::jni::JNIAttach attach;
        JNIEnv *env = attach.get();

        jobject result = extractValue(env, res);
        callConsumerConsume(env, function, result);
        return qi::AnyValue::from<jobject>(nullptr);
    }
};
/**
 * @brief The FunctionFunctorUnwrap struct: P -> Future<R>
 */
struct FunctionFunctorUnwrap
{
    qi::jni::SharedGlobalRef _function;
    /**
     * @brief FunctionFunctorUnwrap::operator () Called by andThenUnwrap
     * @param res Parent future result
     * @return Future of result
     */
    qi::Future<qi::AnyValue> operator()(const qi::AnyValue &res) const
    {
        // Call Java: Function.execute(P)->R
        jobject function = _function.get();

        qi::jni::JNIAttach attach;
        JNIEnv *env = attach.get();

        jobject result = extractValue(env, res);

        jobject futureAnswer = callFunctionExecute(env, function, result);
        return toCppFuture(env, futureAnswer);
    }
};
/**
 * @brief The FutureFunctionFunctor struct: Future<P>->R
 */
struct FutureFunctionFunctor
{
    qi::jni::SharedGlobalRef _argFuture;
    qi::jni::SharedGlobalRef _function;
    /**
     * @brief FutureFunctionFunctor::operator (): Called by then
     * @param QI_UNUSED Future C++
     * @return Function result
     */
    qi::AnyValue operator()(const qi::Future<qi::AnyValue> &) const
    {
        // Call Java: Function.execute(Future<P>)->R
        // we stored the jobject future, so we ignore the parameter
        jobject argFuture = _argFuture.get();
        jobject function = _function.get();

        qi::jni::JNIAttach attach;
        JNIEnv *env = attach.get();

        jobject answer = callFunctionExecute(env, function, argFuture);

        //Convert Java object to AnyValue
        return qi::AnyValue::from<jobject>(answer);
    }
};
/**
 * @brief The FutureFunctionFunctorVoid struct: Future<P>->void
 */
struct FutureFunctionFunctorVoid
{
    qi::jni::SharedGlobalRef _argFuture;
    qi::jni::SharedGlobalRef _function;
    /**
     * @brief FutureFunctionFunctor::operator (): Called by thenVoid
     * @param QI_UNUSED Future C++
     * @return Function result
     */
    qi::AnyValue operator()(const qi::Future<qi::AnyValue> &) const
    {
        // Call Java: Consumer.consume(Future<P>)
        // we stored the jobject future, so we ignore the parameter
        jobject argFuture = _argFuture.get();
        jobject function = _function.get();

        qi::jni::JNIAttach attach;
        JNIEnv *env = attach.get();

        callConsumerConsume(env, function, argFuture);
        return qi::AnyValue::from<jobject>(nullptr);
    }
};
/**
 * @brief The FutureFunctionFunctorUnwrap struct: Future<P>->Future<P>
 */
struct FutureFunctionFunctorUnwrap
{
    qi::jni::SharedGlobalRef _argFuture;
    qi::jni::SharedGlobalRef _function;
    /**
     * @brief FutureFunctionFunctorUnwrap::operator (): called by thenUwrap
     * @param QI_UNUSED Future C++
     * @return Future result
     */
    qi::Future<qi::AnyValue> operator()(const qi::Future<qi::AnyValue> &) const
    {
        // Call Java: Function.execute(Future<P>)->Future<R>
        // we stored the jobject future, so we ignore the parameter
        jobject argFuture = _argFuture.get();
        jobject function = _function.get();

        qi::jni::JNIAttach attach;
        JNIEnv *env = attach.get();

        jobject futureAnswer = callFunctionExecute(env, function, argFuture);
        return toCppFuture(env, futureAnswer);
    }
};

// ***********
// *** JNI ***
// ***********

JNIEXPORT jboolean JNICALL Java_com_aldebaran_qi_Future_qiFutureCallCancel(JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pFuture)
{
    auto * future = futureFromPointer(pFuture);
    // Leave this method as it is (even if returning a boolean doesn't make sense) to avoid breaking
    // projects that were already using it. Future projects should prefer using qiFutureCallCancelRequest.
    future->cancel();
    return true;
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_Future_qiFutureCallCancelRequest(JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pFuture)
{
    futureFromPointer(pFuture)->cancel();
}

JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Future_qiFutureCallGet(JNIEnv *env, jobject QI_UNUSED(obj), jlong pFuture, jint msecs)
{
    return obtainValue(env, futureFromPointer(pFuture), msecs);
}

JNIEXPORT jboolean JNICALL Java_com_aldebaran_qi_Future_qiFutureCallIsCancelled(JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pFuture)
{
    return futureFromPointer(pFuture)->isCanceled();
}

//TODO : return a state instead
JNIEXPORT jboolean JNICALL Java_com_aldebaran_qi_Future_qiFutureCallIsDone(JNIEnv *QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pFuture)
{
    return futureFromPointer(pFuture)->isFinished();
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_Future_qiFutureCallWaitWithTimeout(JNIEnv* QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pFuture, jint timeout)
{
    auto * future = futureFromPointer(pFuture);

    if (timeout)
        future->wait(timeout);
    else
        future->wait();
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_Future_qiFutureCallConnectCallback(JNIEnv *env, jobject thisFuture, jlong pFuture, jobject callback, jint futureCallbackType)
{
    auto * future = futureFromPointer(pFuture);
    auto gThisFuture = qi::jni::makeSharedGlobalRef(env, thisFuture);
    auto gCallback = qi::jni::makeSharedGlobalRef(env, callback);
    qi::FutureCallbackType type = static_cast<qi::FutureCallbackType>(futureCallbackType);
    future->connect(CallbackFunctor{ gThisFuture, gCallback }, type);
}

JNIEXPORT void JNICALL Java_com_aldebaran_qi_Future_qiFutureDestroy(JNIEnv* QI_UNUSED(env), jobject QI_UNUSED(obj), jlong pFuture)
{
    auto * future = futureFromPointer(pFuture);
    delete future;
}

/**
 * Call native future "andThen" for Function(P)->R
 * @param env JNI environment
 * @param thisFuture Caller object
 * @param pFuture Future pointer
 * @param function Function to callback
 * @return Created future
 */
JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Future_qiFutureAndThen(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject function)
{
    auto * future = futureFromPointer(pFuture);
    auto gFunction = qi::jni::makeSharedGlobalRef(env, function);
    const auto fut = toJavaFuture(env, future->andThen(qi::FutureCallbackType_Async,
                                                      FunctionFunctor{gFunction}));
    return env->NewLocalRef(fut.value);
}

/**
 * Call native future "andThen" for Consumer(P)
 * @param env JNI environment
 * @param thisFuture Caller object
 * @param pFuture Future pointer
 * @param function Void function to callback
 * @return Created future
 */
JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Future_qiFutureAndThenVoid(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject function)
{
    auto * future = futureFromPointer(pFuture);
    auto gFunction = qi::jni::makeSharedGlobalRef(env, function);

    const auto fut = toJavaFuture(env, future->andThen(qi::FutureCallbackType_Async,
                                                      FunctionFunctorVoid{gFunction}));
    return env->NewLocalRef(fut.value);
}

/**
 * Call native future "andThen" for Function(P)->Future<R> with automatic unwrap
 * @param env JNI environment
 * @param thisFuture Caller object
 * @param pFuture Future pointer
 * @param function Function to callback
 * @return Created future
 */
JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Future_qiFutureAndThenUnwrap(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject function)
{
    auto * future = futureFromPointer(pFuture);
    auto gFunction = qi::jni::makeSharedGlobalRef(env, function);
    const auto fut = toJavaFuture(env, future->andThen(qi::FutureCallbackType_Async,
                                                      FunctionFunctorUnwrap{gFunction})
                                            .unwrap());
    return env->NewLocalRef(fut.value);
}

/**
 * Call native future "then" for FutureFunction(Future<P>)->R
 * @param env JNI environment
 * @param thisFuture Caller object
 * @param pFuture Future pointer
 * @param futureFunction Function to callback
 * @return Created future
 */
JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Future_qiFutureThen(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject futureFunction)
{
    auto * future = futureFromPointer(pFuture);
    auto gThisFuture = qi::jni::makeSharedGlobalRef(env, thisFuture);
    auto gFunction = qi::jni::makeSharedGlobalRef(env, futureFunction);
    const auto fut = toJavaFuture(env, future->then(qi::FutureCallbackType_Async,
                                                   FutureFunctionFunctor{gThisFuture, gFunction}));
    return env->NewLocalRef(fut.value);
}

/**
 * Call native future "then" for and FutureFunction(Future<P>)->Void
 * @param env JNI environment
 * @param thisFuture Caller object
 * @param pFuture Future pointer
 * @param futureFunction Function to callback
 * @return Created future
 */
JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Future_qiFutureThenVoid(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject futureFunction)
{
    auto * future = futureFromPointer(pFuture);
    auto gThisFuture = qi::jni::makeSharedGlobalRef(env, thisFuture);
    auto gFunction = qi::jni::makeSharedGlobalRef(env, futureFunction);
    const auto fut = toJavaFuture(env, future->then(qi::FutureCallbackType_Async,
                                                   FutureFunctionFunctorVoid{gThisFuture, gFunction}));
    return env->NewLocalRef(fut.value);
}

/**
 * Call native future "then" for Function(Future<P>)->Future<R> with automatic unwrap
 * @param env JNI environment
 * @param thisFuture Caller object
 * @param pFuture Future pointer
 * @param function Function to callback
 * @return Created future
 */
JNIEXPORT jobject JNICALL Java_com_aldebaran_qi_Future_qiFutureThenUnwrap(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject futureFunction)
{
    auto * future = futureFromPointer(pFuture);
    auto gThisFuture = qi::jni::makeSharedGlobalRef(env, thisFuture);
    auto gFunction = qi::jni::makeSharedGlobalRef(env, futureFunction);
    const auto fut = toJavaFuture(env, future->then(qi::FutureCallbackType_Async,
                                                   FutureFunctionFunctorUnwrap{gThisFuture, gFunction})
                                            .unwrap());
    return env->NewLocalRef(fut.value);
}
