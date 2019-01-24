#ifndef _JAVA_JNI_PROPERTY_HPP_
#define _JAVA_JNI_PROPERTY_HPP_

#include <jni.h>

extern "C"
{

  /**
   * Creates a property.
   * @return A pointer to an internal type representing the newly created property.
   */
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Property_createProperty(JNIEnv* env,
                                                                        jclass propertyClass,
                                                                        jclass valueClass);

  /**
   * Creates a property with an initial value.
   * @param value Value to initialize the property with. If it references a null Java object, a
   *        NullPointerException will be thrown.
   * @return A pointer to an internal type representing the newly created property.
   */
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Property_createPropertyWithValue(JNIEnv* env,
                                                                        jclass propertyClass,
                                                                        jclass valueClass,
                                                                        jobject value);

  /**
   * Obtains the value of a property.
   * @param propertyObj The Java Property object.
   * @param pointer Pointer to the result of a previous call to `createProperty`.
   * @return A pointer to the future of the value.
   */
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Property_get(JNIEnv* env,
                                                             jobject propertyObj,
                                                             jlong pointer);

  /**
   * Changes the value of a property.
   * @param propertyObj The Java Property object.
   * @param pointer Pointer to the result of a previous call to `createProperty`.
   * @return Null.
   */
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Property_set(JNIEnv* env,
                                                             jobject propertyObj,
                                                             jlong pointer,
                                                             jobject value);

  /**
   * Destroys a property.
   * @param propertyObj The Java Property object.
   * @param pointer Pointer to the result of a previous call to `createProperty`.
   */
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Property_destroy(JNIEnv* env,
                                                                jobject propertyObj,
                                                                jlong pointer);
}

#endif //_JAVA_JNI_PROPERTY_HPP_
