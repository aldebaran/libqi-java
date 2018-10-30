/*
**
** Author(s):
**  - Pierre ROULLON <proullon@aldebaran-robotics.com>
**
** Copyright (C) 2012, 2013 Aldebaran Robotics
** See COPYING for the license
*/

#include <boost/range/adaptors.hpp>
#include <boost/range/algorithm.hpp>
#include <boost/range/counting_range.hpp>
#include <boost/algorithm/string/join.hpp>
#include <boost/container/small_vector.hpp>

#include <ka/errorhandling.hpp>

#include <qi/log.hpp>
#include <qi/future.hpp>
#include <qi/anyobject.hpp>
#include <qi/type/dynamicobjectbuilder.hpp>
#include <qi/anyfunction.hpp>

#include <callbridge.hpp>
#include <jobjectconverter.hpp>
#include <jnitools.hpp>

qiLogCategory("qimessaging.jni");

MethodInfoHandler gInfoHandler;

using namespace boost::adaptors;
using namespace boost::algorithm;
using boost::counting_range; using boost::range::sort;
using std::begin; using std::end; using boost::size;

namespace
{
  /// Association between a metamethod and some parameters that were prepared for a call.
  struct ParamsBoundMethod
  {
    ParamsBoundMethod(qi::MetaMethod method, std::vector<qi::AnyValue> params)
      : method{ std::move(method) }
      , params{ std::move(params) }
    {}
    qi::MetaMethod method;
    std::vector<qi::AnyValue> params;
  };

  bool greaterCompatibility(const qi::MetaObject::CompatibleMethod& m1,
                            const qi::MetaObject::CompatibleMethod& m2)
  {
    return m1.second > m2.second;
  }

  /// Linearizable R
  /// IteratorType<R> I
  ///
  /// Precondition: i is in [begin(r), end(r)]
  template<typename I, typename R>
  bool isFound(I&& i, R&& r)
  {
    using std::end;
    return std::forward<I>(i) != end(std::forward<R>(r));
  }

  /// Warning: The returned value shares the lifetime of the parameter.
  const qi::SignatureVector& paramsSigs(const qi::MetaMethod& method)
  {
    return method.parametersSignature().children();
  }

  /// Linearizable<qi::MetaMethod> Range1
  /// Linearizable<jobject> Range2
  template<typename Range1, typename Range2>
  std::string suitableMethodNotFoundErrorMessage(JNIEnv& env,
                                                 const std::string& methodNameMaybeWithSig,
                                                 Range1&& candidates,
                                                 Range2&& javaParams)
  {
    std::ostringstream oss;
    oss << "Could not find suitable method '" << methodNameMaybeWithSig
        << "' in the given object for parameter types: (";

    const auto objectClassName = [&](jobject param) -> std::string {
      if (env.IsSameObject(param, nullptr))
        return "<null>";
      const auto clazzName = qi::jni::name(qi::jni::clazz(param));
      return clazzName ? *clazzName : "<unknown>";
    };
    oss << join(transform(std::forward<Range2>(javaParams), objectClassName), ", ")
        << "). Candidates are: ";

    static const auto quotedMethodName = [](const qi::MetaMethod& method) {
      static const auto quote = "'";
      return quote + method.toString() + quote;
    };
    if (boost::empty(candidates))
      oss << "<no candidate found>";
    else
      oss << join(transform(std::forward<Range1>(candidates), quotedMethodName), ", ");
    oss << ".";
    return oss.str();
  }

  /// Returns either a metamethod bound with its parameters, either an error message explaining
  /// why no suitable method could be found.
  ///
  /// Linearizable<qi::MetaMethod> Range1
  /// Linearizable<jobject> Range2
  template<typename Range1, typename Range2>
  boost::variant<ParamsBoundMethod, std::string> suitableMethodOrError(
    JNIEnv& env,
    const std::string& methodNameMaybeWithSig,
    Range1&& candidates,
    Range2&& javaParams)
  {
    // Filter candidates that don't have the expected arity.
    const auto hasExpectedArity = [&](const qi::MetaMethod& method) {
      return paramsSigs(method).size() == javaParams.size();
    };
    const auto expectedArityCandidates = filter(candidates, hasExpectedArity);

    // Convert parameters and bind them to each candidate.
    const auto convertAndBindParams = [&](const qi::MetaMethod& candidate) {
      const auto& paramsSig = paramsSigs(candidate);
      std::vector<qi::AnyValue> params(javaParams.size());
      for (std::size_t i = 0u; i < javaParams.size(); ++i)
      {
        const auto convRef = AnyValue_from_JObject(javaParams[static_cast<jint>(i)], paramsSig[i]);
        params[i].reset(convRef.first, false, true);
      }
      return ParamsBoundMethod{ candidate, params };
    };
    const auto paramsBoundCandidates = transform(expectedArityCandidates, convertAndBindParams);

    // Find the fitting candidate which is the first one for which each bound parameter is
    // convertible to the type of the respective method parameter.
    static const auto isConvertibleTo = [](const qi::AnyValue& ref,
                                           const qi::Signature& targetSig) {
      // We don't use `qi::Signature::isConvertibleTo` because it does not reflect the actual
      // behavior of the `qi::AnyReference::convert` functions, we instead rely directly on these
      // functions.
      return ref.convert(qi::TypeInterface::fromSignature(targetSig))->isValid();
    };
    const auto allBoundParametersAreConvertible = [&](const ParamsBoundMethod& candidate) {
      return !isFound(std::mismatch(begin(candidate.params), end(candidate.params),
                                    begin(paramsSigs(candidate.method)), isConvertibleTo)
                        .first, // first is the iterator to the mismatch element in the first range,
                                // second the one in the second range.
                      candidate.params);
    };
    const auto fittingCandidate = find_if(paramsBoundCandidates, allBoundParametersAreConvertible);
    if (isFound(fittingCandidate, paramsBoundCandidates))
      return *fittingCandidate;
    return suitableMethodNotFoundErrorMessage(env, methodNameMaybeWithSig,
                                              std::forward<Range1>(candidates),
                                              std::forward<Range2>(javaParams));
  }

  /// Tries to call a method if a suitable one is found for this name and parameters, otherwise
  /// returns a future with an error.
  qi::Future<qi::AnyReference> callSuitableMethod(JNIEnv& env,
                                                  const qi::AnyObject& object,
                                                  const std::string& methodNameMaybeWithSig,
                                                  jobjectArray javaParamsArr)
  {
    const auto scopedObjectFromArrayAt = [&](jint i) {
      return ka::scoped(env.GetObjectArrayElement(javaParamsArr, i), &qi::jni::releaseObject);
    };
    const auto scopedJavaParamsGen =
      transform(counting_range<jint>(0, env.GetArrayLength(javaParamsArr)),
                scopedObjectFromArrayAt);
    using ScopedJavaParam = decltype(scopedJavaParamsGen)::value_type;
    const std::vector<ScopedJavaParam> scopedJavaParams{ scopedJavaParamsGen.begin(),
                                                         scopedJavaParamsGen.end() };
    const auto javaParams =
      transform(scopedJavaParams, [](const ScopedJavaParam& param) { return param.value; });

    // We cannot use `qi::MetaObject::findMethod` because:
    //   - The overload that takes no argument needs the exact name (with the signature) which we
    //     are not sure to have here.
    //   - We have to adapt our arguments depending on the target method, so we do not know the
    //     arguments we have before we choose a method to call, thus we cannot use the overload
    //     that takes arguments.
    // So instead we rely on `qi::MetaObject::findCompatibleMethod` which returns all overloads
    // of a method name that might contain a signature.
    const auto sortedCompatibleMethods = [&] {
      auto compatibleMethods = object.metaObject().findCompatibleMethod(methodNameMaybeWithSig);
      return sort(compatibleMethods, greaterCompatibility);
    }();
    static const auto discardCompatibility =
      [](const qi::MetaObject::CompatibleMethod& method) -> const qi::MetaMethod& {
      return method.first;
    };
    const auto candidatesMethods = transform(sortedCompatibleMethods, discardCompatibility);
    const auto methodOrError =
      suitableMethodOrError(env, methodNameMaybeWithSig, candidatesMethods, javaParams);

    if (auto* const errorMsg = boost::get<std::string>(&methodOrError))
      return qi::makeFutureError<qi::AnyReference>(*errorMsg);

    const auto& candidate = boost::get<ParamsBoundMethod>(methodOrError);

    // `qi::Object<T>::metaCall` clones the parameters that are given to it, meaning we can safely
    // destroy the copies we own once the call is done.
    const auto paramsAsReferences =
      transform(candidate.params, std::mem_fn(&qi::AnyValue::asReference));
    return object.metaCall(candidate.method.uid(),
                           qi::AnyReferenceVector{ begin(paramsAsReferences),
                                                   end(paramsAsReferences) });
  }
}

/**
 * @brief call_from_java Calls a function of a `qi.Object` from a Java call.
 */
qi::Future<qi::AnyValue>* call_from_java(JNIEnv* env,
                                         qi::AnyObject object,
                                         const std::string& methodNameMaybeWithSig,
                                         jobjectArray javaParamsArr)
{
  return ka::invoke_catch(
    ka::compose(
      [&](const std::string& msg) {
        throwNewDynamicCallException(env, msg.c_str());
        return nullptr;
      },
      ka::exception_message{}),
    [&]{
      const auto metfut = callSuitableMethod(*env, object, methodNameMaybeWithSig, javaParamsArr);
      qi::Promise<qi::AnyValue> promise;
      qi::adaptFuture(metfut, promise, [](const qi::AnyReference& ref, qi::AnyValue& val) {
        val.reset(ref, false, true);
      });
      return new auto(promise.future());
    });
}

/**
 * @brief object2value Embed a jobject inside a jvalue
 * @param object jobject to embed
 * @return jvalue that embeds the given jobject
 */
jvalue object2value(jobject object)
{
  jvalue value;
  value.l = object;
  return value;
}

/**
 * @brief call_to_java Heller function to call Java methods.
 * @param signature qitype signature formated
 * @param data pointer on a qi_method_info (which hold Java object class and reference)
 * @param params parameters to forward to called method
 * @return
 */
qi::AnyReference call_to_java(std::string signature, void* data, const qi::GenericFunctionParameters& params)
{
  // Arguments given to Java to call NativeTools.callJava method
  // NativeTools.callJava(Object instance, String methodName, String methodSignature, Object[] methodArguments):Object
  jvalue callJavaArguments[4];
  int                 index = 0;
  JNIEnv*             env = 0;
  qi_method_info*     info = reinterpret_cast<qi_method_info*>(data);
  std::vector<std::string>  sigInfo = qi::signatureSplit(signature);

  qi::jni::JNIAttach attach;
  env = attach.get();

  // Check value of method info structure
  if (info == 0)
  {
    qiLogError() << "Internal method informations are not valid";
    throwNewException(env, "Internal method informations are not valid");
    return qi::AnyReference();
  }

  // Translate parameters from AnyValues to jobjects
  qi::GenericFunctionParameters::const_iterator it = params.begin();
  qi::GenericFunctionParameters::const_iterator end = params.end();
  std::vector<qi::TypeInterface*> types;
  jobjectArray arguments = env->NewObjectArray((jsize)params.size(), cls_object, NULL);

  for(; it != end; it++)
  {
    env->SetObjectArrayElement(arguments, index, JObject_from_AnyValue(*it));

    if (it->kind() == qi::TypeKind_Dynamic)
    {
      types.push_back((**it).type());
    }
    else
      types.push_back(it->type());

    index++;
  }

  // Check if function is callable
  qi::Signature from = qi::makeTupleSignature(types);
  qi::Signature to = qi::Signature(sigInfo[2]);

  if (from.isConvertibleTo(to) == 0)
  {
    std::ostringstream ss;
    ss << "cannot convert parameters from " << from.toString() << " to " << to.toString();
    qiLogVerbose() << ss.str();
    throw std::runtime_error(ss.str());
  }

  // Find method class and get methodID
  const auto cls = ka::scoped(qi::jni::clazz(info->instance), qi::jni::releaseClazz);

  if (!cls.value)
  {
    qiLogError() << "Service class not found";
    throw std::runtime_error("Service class not found");
  }

  std::string javaSignature = toJavaSignature(signature);
  callJavaArguments[0] = object2value(info->instance);
  callJavaArguments[1] = object2value(env->NewStringUTF(sigInfo[1].c_str()));
  callJavaArguments[2] = object2value(env->NewStringUTF(javaSignature.c_str()));
  callJavaArguments[3] = object2value(arguments);
  const auto scopedArgs = ka::scoped([&]{
    qi::jni::releaseObject(callJavaArguments[3].l);
    qi::jni::releaseObject(callJavaArguments[2].l);
    qi::jni::releaseObject(callJavaArguments[1].l);
    // callJavaArguments[0].l is not released by purpose: it is info->instance
  });

  const auto valueResult =
    ka::scoped(env->CallStaticObjectMethodA(cls_nativeTools, method_NativeTools_callJava,
                                            callJavaArguments),
               qi::jni::releaseObject);

  qi::jni::handlePendingException(*env);
  return AnyValue_from_JObject(valueResult.value, sigInfo[0]).first;
}

/**
 * @brief event_callback_to_java Generic callback for all events
 * @param vinfo pointer on a qi_method_info (which hold Java object class and reference)
 * @param params parameters to forward to callback
 * @return
 */
qi::AnyReference event_callback_to_java(void *vinfo, const std::vector<qi::AnyReference>& params)
{
  qi_method_info*  info = static_cast<qi_method_info*>(vinfo);

  qiLogVerbose("qimessaging.jni") << "Java event callback called (sig=" << info->sig << ")";

  return call_to_java(info->sig, info, params);
}
