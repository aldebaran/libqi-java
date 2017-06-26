package com.aldebaran.qi;

import java.lang.annotation.*;

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
