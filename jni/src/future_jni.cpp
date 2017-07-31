/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#include <qi/anyvalue.hpp>

#include <jnitools.hpp>
#include <futurehandler.hpp>
#include <future_jni.hpp>
#include <callbridge.hpp>

qiLogCategory("qimessaging.java");

// *************
// *** TOOLS ***
// *************

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

        if(typeInterface->info() == qi::typeOf<qi::AnyValue>()->info())
        {
            //If the AnyReference contains an AnyValue,
            //Then we get the value from this AnyValue
            return extractValue(env, *reinterpret_cast<qi::AnyValue*>(arRes.rawValue()));
        }

        std::pair<qi::AnyReference, bool> converted = arRes.convert(qi::typeOf<jobject>());

        //If the converted value doesn't have a valid type, trying to obtain its rawValue will do a SIGSEGV,
        // because the method rawValue() refers to a non initialized value in this case
        if(! converted.first.type())
        {
            //Not valid, return "null" object to Java
            return nullptr;
        }

        //The converted value is valid
        jobject result = * reinterpret_cast<jobject*>(converted.first.rawValue());
        // keep it alive while we remove the global ref
        result = env->NewLocalRef(result);

        if (converted.second)
        {
            converted.first.destroy();
        }

        return result;
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
    checkJavaExceptionAndReport(env);

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
    checkJavaExceptionAndReport(env);
}

/**
 * @brief obtainFutureCfromFutureJava Obtain C++ future linked to given Java Future
 * @param env JNI environment
 * @param future Java Future
 * @return C future linked
 */
static qi::Future<qi::AnyValue> obtainFutureCfromFutureJava(JNIEnv *env, jobject future)
{
    // Obtain the C++ pointer embed in Future Java
    jlong pointer = env->GetLongField(future, field_future_pointer);

    //Check if exception happened on Java side
    checkJavaExceptionAndReport(env);

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

        //Treatment if answer is null
        if (env->IsSameObject(answer, NULL))
        {
            return qi::AnyValue(qi::AnyReference(qi::typeOf<void>()));
        }

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
    void operator()(const qi::AnyValue & res) const
    {
        // Call Java: Consumer.consume(P)->void
        jobject function = _function.get();

        qi::jni::JNIAttach attach;
        JNIEnv *env = attach.get();

        jobject result = extractValue(env, res);
        callConsumerConsume(env, function, result);
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
        return obtainFutureCfromFutureJava(env, futureAnswer);
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

        //Special treatement for null value
        if (env->IsSameObject(answer, NULL))
        {
            return qi::AnyValue(qi::AnyReference(qi::typeOf<void>()));
        }

        //Convert Java object to AnyValue
        return qi::AnyValue::from<jobject>(answer);
    }
};
/**
 * @brief The FutureFunctionFunctor struct: Future<P>->void
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
    void operator()(const qi::Future<qi::AnyValue> &) const
    {
        // Call Java: Consumer.consume(Future<P>)
        // we stored the jobject future, so we ignore the parameter
        jobject argFuture = _argFuture.get();
        jobject function = _function.get();

        qi::jni::JNIAttach attach;
        JNIEnv *env = attach.get();

        callConsumerConsume(env, function, argFuture);
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
        return obtainFutureCfromFutureJava(env, futureAnswer);
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
    if (future->isCancelable() == false)
    {
        return false;
    }

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
JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureAndThen(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject function)
{
    auto * future = futureFromPointer(pFuture);
    auto gFunction = qi::jni::makeSharedGlobalRef(env, function);
    qi::Future<qi::AnyValue> result = future->andThen(qi::FutureCallbackType_Async,
                                                      FunctionFunctor{gFunction});
    std::unique_ptr<qi::Future<qi::AnyValue>> resultPointer(new auto(result));
    return reinterpret_cast<jlong>(resultPointer.release());
}

/**
 * Call native future "andThen" for Consumer(P)
 * @param env JNI environment
 * @param thisFuture Caller object
 * @param pFuture Future pointer
 * @param function Void function to callback
 * @return Created future
 */
JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureAndThenVoid(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject function)
{
    auto * future = futureFromPointer(pFuture);
    auto gFunction = qi::jni::makeSharedGlobalRef(env, function);
    qi::Future<void> result = future->andThen(qi::FutureCallbackType_Async,
                                              FunctionFunctorVoid{gFunction});
    std::unique_ptr<qi::Future<void>> resultPointer(new auto(result));
    return reinterpret_cast<jlong>(resultPointer.release());
}

/**
 * Call native future "andThen" for Function(P)->Future<R> with automatic unwrap
 * @param env JNI environment
 * @param thisFuture Caller object
 * @param pFuture Future pointer
 * @param function Function to callback
 * @return Created future
 */
JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureAndThenUnwrap(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject function)
{
    auto * future = futureFromPointer(pFuture);
    auto gFunction = qi::jni::makeSharedGlobalRef(env, function);
    qi::Future<qi::AnyValue> result = future->andThen(qi::FutureCallbackType_Async,
                                                      FunctionFunctorUnwrap{gFunction})
                                            .unwrap();
    std::unique_ptr<qi::Future<qi::AnyValue>> resultPointer(new auto(result));
    return reinterpret_cast<jlong>(resultPointer.release());
}

/**
 * Call native future "then" for FutureFunction(Future<P>)->R
 * @param env JNI environment
 * @param thisFuture Caller object
 * @param pFuture Future pointer
 * @param futureFunction Function to callback
 * @return Created future
 */
JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureThen(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject futureFunction)
{
    auto * future = futureFromPointer(pFuture);
    auto gThisFuture = qi::jni::makeSharedGlobalRef(env, thisFuture);
    auto gFunction = qi::jni::makeSharedGlobalRef(env, futureFunction);
    qi::Future<qi::AnyValue> result = future->then(qi::FutureCallbackType_Async,
                                                   FutureFunctionFunctor{gThisFuture, gFunction});
    std::unique_ptr<qi::Future<qi::AnyValue>> resultPointer(new auto(result));
    return reinterpret_cast<jlong>(resultPointer.release());
}

/**
 * Call native future "then" for and FutureFunction(Future<P>)->Void
 * @param env JNI environment
 * @param thisFuture Caller object
 * @param pFuture Future pointer
 * @param futureFunction Function to callback
 * @return Created future
 */
JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureThenVoid(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject futureFunction)
{
    auto * future = futureFromPointer(pFuture);
    auto gThisFuture = qi::jni::makeSharedGlobalRef(env, thisFuture);
    auto gFunction = qi::jni::makeSharedGlobalRef(env, futureFunction);
    qi::Future<void> result = future->then(qi::FutureCallbackType_Async,
                                           FutureFunctionFunctorVoid{gThisFuture, gFunction});
    std::unique_ptr<qi::Future<void>> resultPointer(new auto(result));
    return reinterpret_cast<jlong>(resultPointer.release());
}

/**
 * Call native future "then" for Function(Future<P>)->Future<R> with automatic unwrap
 * @param env JNI environment
 * @param thisFuture Caller object
 * @param pFuture Future pointer
 * @param function Function to callback
 * @return Created future
 */
JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Future_qiFutureThenUnwrap(JNIEnv* env, jobject thisFuture, jlong pFuture, jobject futureFunction)
{
    auto * future = futureFromPointer(pFuture);
    auto gThisFuture = qi::jni::makeSharedGlobalRef(env, thisFuture);
    auto gFunction = qi::jni::makeSharedGlobalRef(env, futureFunction);
    qi::Future<qi::AnyValue> result = future->then(qi::FutureCallbackType_Async,
                                                   FutureFunctionFunctorUnwrap{gThisFuture, gFunction})
                                            .unwrap();
    std::unique_ptr<qi::Future<qi::AnyValue>> resultPointer(new auto(result));
    return reinterpret_cast<jlong>(resultPointer.release());
}
