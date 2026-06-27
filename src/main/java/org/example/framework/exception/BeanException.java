package org.example.framework.exception;

/**
 * Thrown when the DI container cannot create or inject a bean.
 */
public class BeanException extends RuntimeException {

    public BeanException(String message) {
        super(message);
    }

    public BeanException(String message, Throwable cause) {
        super(message, cause);
    }
}
