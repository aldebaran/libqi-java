#ifndef _JAVA_JNI_QITOOLS_HPP_
#define _JAVA_JNI_QITOOLS_HPP_

#include <qi/future.hpp>

namespace qi {

  using ThenCallback = std::function<qi::Future<qi::AnyValue>(qi::Future<qi::AnyValue>)>;
  using AndThenCallback = std::function<qi::Future<qi::AnyValue>(qi::AnyValue)>;

  /**
   * @brief Equivalent to future.then(callback), but that does not serialize
   * data (due to unwrap()), so that the type need not to be known by libqi.
   *
   * Therefore, an instance of a custom Java class can be returned as the future
   * value from a QiFunction.
   */
  qi::Future<qi::AnyValue> localThen(qi::Future<qi::AnyValue> &future, const ThenCallback &callback);

  /**
   * @brief Equivalent to future.andThen(callback), but that does not serialize
   * data (due to unwrap()), so that the type need not to be known by libqi.
   *
   * Therefore, an instance of a custom Java class can be returned as the future
   * value from a QiFunction.
   */
  qi::Future<qi::AnyValue> localAndThen(qi::Future<qi::AnyValue> &future, const AndThenCallback &callback);

} // namespace qi

#endif // !_JAVA_JNI_QITOOLS_HPP_
