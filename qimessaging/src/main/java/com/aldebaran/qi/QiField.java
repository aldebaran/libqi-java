package com.aldebaran.qi;

import java.lang.annotation.*;

/**
 * Annotation indicating that a field represents an element inside a
 * {@link Tuple}. Each field must indicate the index it matches in the
 * {@link Tuple} the {@link QiStruct} represents.
 *
 * @see QiStruct
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface QiField {
    int value();
}
