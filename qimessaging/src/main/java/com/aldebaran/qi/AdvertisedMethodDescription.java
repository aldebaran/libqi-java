package com.aldebaran.qi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to add description to method inside interface managed by
 * advertised method:
 * {@link DynamicObjectBuilder#advertiseMethods(Class, Object)}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface AdvertisedMethodDescription {
    /**
     * Method description
     *
     * @return Method description
     */
    String value();
}
