
#ifndef QI_JAVA_JNI_OPTIONAL_HPP
#define QI_JAVA_JNI_OPTIONAL_HPP

#include <jni.h>

struct OptionalInitFromInstance{};
struct OptionalInitFromValue{};

constexpr OptionalInitFromInstance optionalInitFromInstance  = OptionalInitFromInstance{};
constexpr OptionalInitFromValue optionalInitFromValue        = OptionalInitFromValue{};

/**
 * @brief The JNIOptional class Helper class to manipulation Java Optional<?> in C++
 */
class JNIOptional
{
  public:
    JNIOptional();
    JNIOptional(JNIOptional const &) = delete;
    JNIOptional(JNIOptional &&) = delete;

    JNIOptional(OptionalInitFromInstance, jobject obj);
    JNIOptional(OptionalInitFromValue, jobject value);

    JNIOptional& operator=(JNIOptional const &) = delete;
    JNIOptional& operator=(JNIOptional &&) = delete;

    ~JNIOptional();

    jobject value() const;
    
    bool hasValue() const;

    jobject object() const;
    
  private:
    jobject _obj;
    JNIEnv* _env;
};

#endif // !QI_JAVA_JNI_OPTIONAL_HPP
