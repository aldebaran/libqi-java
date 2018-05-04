#include "property.hpp"
#include <qi/property.hpp>
#include <qi/future.hpp>
#include "jnitools.hpp"

/**
 * Create a property
 * @param env JNI environment
 * @param obj Object source
 * @return Pointer on created property
 */
JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Property_createProperty(JNIEnv * obj, jobject clazz)
{
    auto propertyPointer = new PropertyManager();
    return reinterpret_cast<jlong>(propertyPointer);
}

/**
 * Obtain a property value
 * @param env JNI environment
 * @param obj Object source
 * @param pointer Property pointer
 * @return Pointer on future to get the value
 */
JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Property_get(JNIEnv * env, jobject obj, jlong pointer)
{
    auto propertyManager = reinterpret_cast<PropertyManager *>(pointer);
    auto propertyPointer = propertyManager->property;
    // Potential global reference issue here, due multithread, Garbage collector and other fun.
    auto futurePointer = new qi::Future<qi::AnyValue> { propertyPointer->value().async() };
    return reinterpret_cast<jlong>(futurePointer);
}

/**
 * Change property value
 * @param env JNI environment
 * @param obj Object source
 * @param pointer Property pointer
 * @param value New value
 * @return Pointer on future for know when property effectively set
 */
JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Property_set(JNIEnv * env, jobject obj, jlong pointer, jobject value)
{
    auto propertyManager = reinterpret_cast<PropertyManager *>(pointer);
    propertyManager->setValue(env, value);
    auto propertyPointer = propertyManager->property;
    // Have wait the result here.
    // When embed inside a future, it happen time to time some crash.
    propertyPointer->setValue(qi::AnyValue::from<jobject>(propertyManager->goblaReference)).wait();
    //Keep to keep Java signaute, must be change later.
    return 0;
}

/**
 * Destroy a property
 * @param env JNI environment
 * @param obj Object source
 * @param pointer Property pointer
 */
JNIEXPORT void JNICALL Java_com_aldebaran_qi_Property_destroy(JNIEnv * env, jobject obj, jlong pointer)
{
    auto propertyManager = reinterpret_cast<PropertyManager *>(pointer);
    propertyManager->destroy(env);
    delete propertyManager;
}
