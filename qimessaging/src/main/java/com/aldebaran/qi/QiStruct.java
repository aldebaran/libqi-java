package com.aldebaran.qi;

import java.lang.annotation.*;

/**
 * Annotation indicating that a class represents a conversion from a
 * {@link Tuple}.
 * <p>
 * A class having this annotation must add the {@code QiField} annotation to
 * each of its fields, indicating the index they match in the {@link Tuple} the
 * class represents.
 * <p>
 * Here is an example:
 * <p>
 * <pre>
 * &#64;QiStruct
 * class Person {
 *     &#64;QiField(0)
 *     String firstName;
 *     &#64;QiField(1)
 *     String lastName;
 * }
 * </pre>
 * <p>
 * Field names are irrelevant.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface QiStruct {
    // empty
}
