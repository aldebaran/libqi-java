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
 * A class having this annotation must declare fields in the same order as the
 * one in the Tuple it represents.
 *
 * For instance, if it must be created from a Tuple of
 * <code>{ int, String }</code>, it must first declare a field of type
 * {@code int} then a field having type {@code String}.
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
