package org.example.framework.annotations;

import java.lang.annotation.*;

/**
 * Marks a constructor, setter, or field as an injection point.
 * The container resolves and injects the matching bean automatically.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface Autowired {
    boolean required() default true;
}
