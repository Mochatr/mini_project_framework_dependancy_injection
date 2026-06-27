package org.example.framework.core;

/**
 * Central interface for the DI container.
 * Provides bean lookup by name or by type.
 */
public interface ApplicationContext {

    /**
     * Returns the bean registered under the given name.
     *
     * @param name the bean id
     * @return the managed bean instance
     */
    Object getBean(String name);

    /**
     * Returns the bean whose type is assignable to {@code type}.
     * Throws if zero or more than one candidate is found.
     *
     * @param type the required type
     * @param <T>  inferred return type
     * @return the managed bean instance
     */
    <T> T getBean(Class<T> type);
}
