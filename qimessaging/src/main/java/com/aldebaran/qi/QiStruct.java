package com.aldebaran.qi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that a class represent a conversion from a
 * {@link Tuple}.
 *
 * A class having this annotation must annotate fields with {@code QiField} with
 * the matching index in the Tuple it represents.
 *
 * Here is an example:
 * <pre>
 * @QiStruct
 * class Person {
 *     @QiField(0)
 *     String firstName;
 *     @QiField(1)
 *     String lastName;
 * }
 * </pre>
 *
 * Field names are irrelevant.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface QiStruct
{
  // empty
}
