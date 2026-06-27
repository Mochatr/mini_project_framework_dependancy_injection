package org.example.framework.core;

import org.example.framework.annotations.*;
import org.example.framework.exception.BeanException;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.*;
import java.util.*;

/**
 * ApplicationContext that discovers beans by scanning a base package for
 * classes annotated with {@link Component}.
 *
 * <p>Supports three injection strategies, each driven by {@link Autowired}:
 * <ol>
 *   <li><b>Constructor injection</b> — {@code @Autowired} on a constructor.</li>
 *   <li><b>Setter injection</b> — {@code @Autowired} on a setter method.</li>
 *   <li><b>Field injection</b> — {@code @Autowired} directly on a field.</li>
 * </ol>
 *
 * <p>{@link Qualifier} narrows the candidate when multiple beans match a type.
 * {@link Value} injects a literal String / primitive into fields or setter parameters.
 *
 * <p>Usage:
 * <pre>{@code
 * ApplicationContext ctx = new AnnotationApplicationContext("org.example.app");
 * IMetier metier = ctx.getBean(IMetier.class);
 * }</pre>
 */
public class AnnotationApplicationContext implements ApplicationContext {

    /** Bean name → singleton instance. */
    private final Map<String, Object> singletons = new LinkedHashMap<>();

    public AnnotationApplicationContext(String basePackage) {
        Set<Class<?>> components = scanComponents(basePackage);
        // Instantiate all (constructor injection happens here)
        for (Class<?> clazz : components) {
            getOrCreate(clazz, components);
        }
        // Inject setters and fields after all instances exist
        for (Object bean : singletons.values()) {
            injectSetters(bean, components);
            injectFields(bean, components);
        }
    }

    // -----------------------------------------------------------------------
    // ApplicationContext
    // -----------------------------------------------------------------------

    @Override
    public Object getBean(String name) {
        Object bean = singletons.get(name);
        if (bean == null) {
            throw new BeanException("No bean named '" + name + "' found");
        }
        return bean;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        List<Object> candidates = singletons.values().stream()
                .filter(b -> type.isAssignableFrom(b.getClass()))
                .toList();
        if (candidates.isEmpty()) {
            throw new BeanException("No bean of type '" + type.getName() + "' found");
        }
        if (candidates.size() > 1) {
            throw new BeanException("Multiple beans of type '" + type.getName()
                    + "' found — use @Qualifier or getBean(name)");
        }
        return (T) candidates.get(0);
    }

    // -----------------------------------------------------------------------
    // Scanning
    // -----------------------------------------------------------------------

    private Set<Class<?>> scanComponents(String basePackage) {
        Reflections reflections = new Reflections(basePackage, Scanners.TypesAnnotated);
        return reflections.getTypesAnnotatedWith(Component.class);
    }

    // -----------------------------------------------------------------------
    // Instantiation (constructor injection)
    // -----------------------------------------------------------------------

    private Object getOrCreate(Class<?> clazz, Set<Class<?>> allComponents) {
        String name = beanName(clazz);
        if (singletons.containsKey(name)) {
            return singletons.get(name);
        }
        Object bean = instantiate(clazz, allComponents);
        singletons.put(name, bean);
        return bean;
    }

    private Object instantiate(Class<?> clazz, Set<Class<?>> allComponents) {
        // Look for an @Autowired constructor
        for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
            if (ctor.isAnnotationPresent(Autowired.class)) {
                Object[] args = resolveParameters(ctor.getParameters(), allComponents);
                try {
                    ctor.setAccessible(true);
                    return ctor.newInstance(args);
                } catch (Exception e) {
                    throw new BeanException("Constructor injection failed for "
                            + clazz.getName(), e);
                }
            }
        }
        // Fall back to no-arg constructor
        try {
            Constructor<?> noArg = clazz.getDeclaredConstructor();
            noArg.setAccessible(true);
            return noArg.newInstance();
        } catch (Exception e) {
            throw new BeanException("No suitable constructor for " + clazz.getName(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Setter injection
    // -----------------------------------------------------------------------

    private void injectSetters(Object bean, Set<Class<?>> allComponents) {
        for (Method method : bean.getClass().getMethods()) {
            if (!method.isAnnotationPresent(Autowired.class)) continue;
            if (method.getParameterCount() == 0) continue;
            Object[] args = resolveParameters(method.getParameters(), allComponents);
            try {
                method.invoke(bean, args);
            } catch (Exception e) {
                throw new BeanException("Setter injection failed: " + method.getName()
                        + " on " + bean.getClass().getName(), e);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Field injection
    // -----------------------------------------------------------------------

    private void injectFields(Object bean, Set<Class<?>> allComponents) {
        Class<?> clazz = bean.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    field.setAccessible(true);
                    Object dep = resolveByTypeAndQualifier(
                            field.getType(),
                            field.getAnnotation(Qualifier.class),
                            allComponents);
                    try { field.set(bean, dep); }
                    catch (IllegalAccessException e) {
                        throw new BeanException("Field injection failed: " + field.getName()
                                + " on " + bean.getClass().getName(), e);
                    }
                } else if (field.isAnnotationPresent(Value.class)) {
                    field.setAccessible(true);
                    String raw = field.getAnnotation(Value.class).value();
                    try { field.set(bean, coerce(raw, field.getType())); }
                    catch (IllegalAccessException e) {
                        throw new BeanException("@Value injection failed: " + field.getName(), e);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    // -----------------------------------------------------------------------
    // Parameter resolution (shared by constructor & setter)
    // -----------------------------------------------------------------------

    private Object[] resolveParameters(Parameter[] params, Set<Class<?>> allComponents) {
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            if (p.isAnnotationPresent(Value.class)) {
                args[i] = coerce(p.getAnnotation(Value.class).value(), p.getType());
            } else {
                args[i] = resolveByTypeAndQualifier(
                        p.getType(),
                        p.getAnnotation(Qualifier.class),
                        allComponents);
            }
        }
        return args;
    }

    private Object resolveByTypeAndQualifier(Class<?> type, Qualifier qualifier,
                                              Set<Class<?>> allComponents) {
        if (qualifier != null) {
            Object bean = singletons.get(qualifier.value());
            if (bean == null) {
                // May not be created yet — find and create it
                Class<?> target = allComponents.stream()
                        .filter(c -> qualifier.value().equals(beanName(c)))
                        .findFirst()
                        .orElseThrow(() -> new BeanException(
                                "No bean with name '" + qualifier.value() + "' found"));
                bean = getOrCreate(target, allComponents);
            }
            return bean;
        }
        // Resolve by type
        List<Object> candidates = singletons.values().stream()
                .filter(b -> type.isAssignableFrom(b.getClass()))
                .toList();
        if (candidates.size() == 1) return candidates.get(0);
        if (candidates.isEmpty()) {
            // Not yet created — find matching component
            Class<?> target = allComponents.stream()
                    .filter(c -> type.isAssignableFrom(c))
                    .filter(c -> !c.isInterface())
                    .findFirst()
                    .orElseThrow(() -> new BeanException(
                            "No bean of type '" + type.getName() + "' found"));
            return getOrCreate(target, allComponents);
        }
        throw new BeanException("Multiple beans of type '" + type.getName()
                + "' — add @Qualifier to disambiguate");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    static String beanName(Class<?> clazz) {
        Component ann = clazz.getAnnotation(Component.class);
        if (ann != null && !ann.value().isEmpty()) return ann.value();
        String simple = clazz.getSimpleName();
        return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
    }

    @SuppressWarnings("unchecked")
    private <T> T coerce(String raw, Class<T> target) {
        if (target == String.class)                             return (T) raw;
        if (target == int.class    || target == Integer.class) return (T) Integer.valueOf(raw);
        if (target == long.class   || target == Long.class)    return (T) Long.valueOf(raw);
        if (target == double.class || target == Double.class)  return (T) Double.valueOf(raw);
        if (target == boolean.class|| target == Boolean.class) return (T) Boolean.valueOf(raw);
        return (T) raw;
    }
}
