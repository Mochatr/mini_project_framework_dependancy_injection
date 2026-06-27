package org.example.framework.annotations;

import java.lang.annotation.*;

/**
 * Disambiguates which bean to inject when multiple candidates exist
 * for a given type. Used alongside {@link Autowired}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Documented
public @interface Qualifier {
    String value();
}
