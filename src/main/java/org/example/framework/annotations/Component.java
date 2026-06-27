package org.example.framework.annotations;

import java.lang.annotation.*;

/**
 * Marks a class as a bean managed by the DI container.
 * The optional value sets the bean name; defaults to the simple class name
 * with the first letter lowercased.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Component {
    String value() default "";
}
