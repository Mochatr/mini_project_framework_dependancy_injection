package org.example.framework.annotations;

import java.lang.annotation.*;

/**
 * Injects a primitive or String value into a field or setter parameter.
 * Example: {@code @Value("42")} or {@code @Value("hello")}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Documented
public @interface Value {
    String value();
}
