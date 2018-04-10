#ifndef _JAVA_JNI_PROPERTY_HPP_
#define _JAVA_JNI_PROPERTY_HPP_

#include <jni.h>

extern "C"
{
  /**
   * Create a property
   * @param env JNI environment
   * @param obj Object source
   * @return Pointer on creater property
   */
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Property_createProperty(JNIEnv * env, jobject obj);

  /**
   * Obtain a property value
   * @param env JNI environment
   * @param obj Object source
   * @param pointer Property pointer
   * @return Pointer on future to get the value
   */
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Property_get(JNIEnv * env, jobject obj, jlong pointer);

  /**
   * Change property value
   * @param env JNI environment
   * @param obj Object source
   * @param pointer Property pointer
   * @param value New value
   * @return Pointer on future for knnow when property effectively set
   */
  JNIEXPORT jlong JNICALL Java_com_aldebaran_qi_Property_set(JNIEnv * env, jobject obj, jlong pointer, jobject value);

  /**
   * Destroy a property
   * @param env JNI environment
   * @param obj Object source
   * @param pointer Property pointer
   */
  JNIEXPORT void JNICALL Java_com_aldebaran_qi_Property_destroy(JNIEnv * env, jobject obj, jlong pointer);
}

#endif //_JAVA_JNI_PROPERTY_HPP_
