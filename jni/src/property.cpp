#include "property.hpp"
#include <qi/property.hpp>
#include <qi/future.hpp>
#include "jnitools.hpp"

/**
 * Create a property
 * @param env JNI environment
 * @param clazz Property java class
 * @return Pointer on created property
 */
JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Property_createProperty(JNIEnv * env, jobject clazz)
{
    qiLogError("NONO") << "-> Java_com_aldebaran_qi_Property_createProperty";
    auto propertyPointer = new qi::Property<qi::AnyValue>();
    qiLogError("NONO") << "<- Java_com_aldebaran_qi_Property_createProperty";
    return reinterpret_cast<jlong>(propertyPointer);
}

/**
 * Obtain a property value
 * @param env JNI environment
 * @param clazz Property java class
 * @param pointer Property pointer
 * @return Pointer on future to get the value
 */
JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Property_get(JNIEnv * env, jobject clazz, jlong pointer)
{
    qiLogError("NONO") << "-> Java_com_aldebaran_qi_Property_get : " << pointer;
    auto propertyPointer = reinterpret_cast<qi::Property<qi::AnyValue> *>(pointer);
    auto futurePointer = new qi::Future<qi::AnyValue> { propertyPointer->value().async() };
    qiLogError("NONO") << "<- Java_com_aldebaran_qi_Property_get";
    return reinterpret_cast<jlong>(futurePointer);
}

/**
 * Change property value
 * @param env JNI environment
 * @param clazz Property java class
 * @param pointer Property pointer
 * @param value New value
 * @return Pointer on future for know when property effectively set
 */
JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Property_set(JNIEnv * env, jobject clazz, jlong pointer, jobject value)
{
    qiLogError("NONO") << "-> Java_com_aldebaran_qi_Property_set : " << pointer;
    auto propertyPointer = reinterpret_cast<qi::Property<qi::AnyValue> *>(pointer);
    auto futurePointer = new qi::Future<void> { propertyPointer->setValue(qi::AnyValue::from<jobject>(value)) };
    qiLogError("NONO") << "<- Java_com_aldebaran_qi_Property_set";
    return reinterpret_cast<jlong>(futurePointer);
}

/**
 * Destroy a property
 * @param env JNI environment
 * @param clazz Property java class
 * @param pointer Property pointer
 */
JNIEXPORT void JNICALL Java_com_aldebaran_qi_Property_destroy(JNIEnv * env, jobject clazz, jlong pointer)
{
    qiLogError("NONO") << "-> Java_com_aldebaran_qi_Property_destroy : " << pointer;
    auto propertyPointer = reinterpret_cast<qi::Property<qi::AnyValue> *>(pointer);
    delete propertyPointer;
    qiLogError("NONO") << "<- Java_com_aldebaran_qi_Property_destroy";
}
