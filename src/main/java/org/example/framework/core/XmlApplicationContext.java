package org.example.framework.core;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.example.framework.exception.BeanException;
import org.example.framework.xml.*;

import java.io.InputStream;
import java.lang.reflect.*;
import java.util.*;

/**
 * ApplicationContext backed by an XML configuration file (JAXB/OXM).
 *
 * <p>Supports three injection strategies per bean:
 * <ol>
 *   <li>Constructor injection via {@code <constructor-arg ref="..." />} or {@code value="..."}</li>
 *   <li>Setter injection via {@code <property name="..." ref="..." />} or {@code value="..."}</li>
 *   <li>Field injection via {@code <property name="..." ref="..." />} or {@code value="..."}
 *       when no matching setter exists (direct field access).</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>{@code
 * ApplicationContext ctx = new XmlApplicationContext("beans.xml");
 * IMetier metier = ctx.getBean("metier", IMetier.class);
 * }</pre>
 */
public class XmlApplicationContext implements ApplicationContext {

    /** Bean name → singleton instance. */
    private final Map<String, Object> singletons = new LinkedHashMap<>();

    /** Bean name → its XML definition (kept for deferred ref resolution). */
    private final Map<String, BeanDefinition> definitions = new LinkedHashMap<>();

    public XmlApplicationContext(String xmlResource) {
        BeansConfig config = parseXml(xmlResource);
        // First pass: collect definitions
        for (BeanDefinition bd : config.getBeans()) {
            definitions.put(bd.getId(), bd);
        }
        // Second pass: instantiate all beans
        for (String id : definitions.keySet()) {
            getOrCreate(id);
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
            throw new BeanException("Multiple beans of type '" + type.getName() + "' found");
        }
        return (T) candidates.get(0);
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private Object getOrCreate(String id) {
        if (singletons.containsKey(id)) {
            return singletons.get(id);
        }
        BeanDefinition bd = definitions.get(id);
        if (bd == null) {
            throw new BeanException("No bean definition found for id '" + id + "'");
        }
        Object bean = instantiate(bd);
        singletons.put(id, bean);
        injectProperties(bean, bd);
        return bean;
    }

    /** Instantiates the bean using constructor-arg definitions if present. */
    private Object instantiate(BeanDefinition bd) {
        try {
            Class<?> clazz = Class.forName(bd.getClassName());
            List<ConstructorArgDefinition> args = bd.getConstructorArgs();
            if (args.isEmpty()) {
                return clazz.getDeclaredConstructor().newInstance();
            }
            // Resolve constructor arguments
            Object[] argValues = resolveConstructorArgs(args);
            Class<?>[] argTypes = Arrays.stream(argValues)
                    .map(Object::getClass)
                    .toArray(Class[]::new);
            Constructor<?> ctor = findConstructor(clazz, argTypes, args);
            ctor.setAccessible(true);
            return ctor.newInstance(argValues);
        } catch (BeanException e) {
            throw e;
        } catch (Exception e) {
            throw new BeanException("Cannot instantiate bean '" + bd.getId() + "'", e);
        }
    }

    private Object[] resolveConstructorArgs(List<ConstructorArgDefinition> args) {
        Object[] values = new Object[args.size()];
        for (int i = 0; i < args.size(); i++) {
            ConstructorArgDefinition arg = args.get(i);
            if (arg.getRef() != null) {
                values[i] = getOrCreate(arg.getRef());
            } else {
                values[i] = arg.getValue();
            }
        }
        return values;
    }

    /**
     * Finds a constructor whose parameter types are assignable from the resolved argument types.
     * Falls back to type attribute hints when raw String values are used.
     */
    private Constructor<?> findConstructor(Class<?> clazz, Class<?>[] argTypes,
                                           List<ConstructorArgDefinition> argDefs) {
        for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length != argTypes.length) continue;
            boolean match = true;
            for (int i = 0; i < params.length; i++) {
                if (!params[i].isAssignableFrom(argTypes[i])) {
                    // Allow primitive/String coercion when a literal value was given
                    if (argDefs.get(i).getValue() != null && canCoerce(params[i])) continue;
                    match = false;
                    break;
                }
            }
            if (match) return ctor;
        }
        throw new BeanException("No matching constructor found in " + clazz.getName());
    }

    /** Injects properties via setter or direct field access. */
    private void injectProperties(Object bean, BeanDefinition bd) {
        for (PropertyDefinition prop : bd.getProperties()) {
            Object value = prop.getRef() != null
                    ? getOrCreate(prop.getRef())
                    : coerce(prop.getValue(), fieldType(bean.getClass(), prop.getName()));
            if (!trySetter(bean, prop.getName(), value)) {
                setField(bean, prop.getName(), value);
            }
        }
    }

    /** Attempts setter injection; returns true if a setter was found and called. */
    private boolean trySetter(Object bean, String propertyName, Object value) {
        String setterName = "set" + Character.toUpperCase(propertyName.charAt(0))
                + propertyName.substring(1);
        for (Method m : bean.getClass().getMethods()) {
            if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
                try {
                    m.invoke(bean, coerce(value, m.getParameterTypes()[0]));
                    return true;
                } catch (Exception e) {
                    throw new BeanException("Setter injection failed for property '"
                            + propertyName + "' on " + bean.getClass().getName(), e);
                }
            }
        }
        return false;
    }

    /** Direct field injection (bypasses access control). */
    private void setField(Object bean, String fieldName, Object value) {
        try {
            Field f = findField(bean.getClass(), fieldName);
            f.setAccessible(true);
            f.set(bean, coerce(value, f.getType()));
        } catch (Exception e) {
            throw new BeanException("Field injection failed for '" + fieldName
                    + "' on " + bean.getClass().getName(), e);
        }
    }

    private Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        while (clazz != null) {
            try { return clazz.getDeclaredField(name); }
            catch (NoSuchFieldException ignore) { clazz = clazz.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    private Class<?> fieldType(Class<?> clazz, String name) {
        try { return findField(clazz, name).getType(); }
        catch (NoSuchFieldException e) { return String.class; }
    }

    // -----------------------------------------------------------------------
    // Type coercion helpers
    // -----------------------------------------------------------------------

    private boolean canCoerce(Class<?> type) {
        return type == String.class || type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == double.class || type == Double.class
                || type == boolean.class || type == Boolean.class;
    }

    @SuppressWarnings("unchecked")
    private <T> T coerce(Object raw, Class<T> target) {
        if (raw == null || target.isInstance(raw)) return (T) raw;
        if (!(raw instanceof String s)) return (T) raw;
        if (target == int.class    || target == Integer.class) return (T) Integer.valueOf(s);
        if (target == long.class   || target == Long.class)    return (T) Long.valueOf(s);
        if (target == double.class || target == Double.class)  return (T) Double.valueOf(s);
        if (target == boolean.class|| target == Boolean.class) return (T) Boolean.valueOf(s);
        return (T) s;
    }

    // -----------------------------------------------------------------------
    // XML parsing
    // -----------------------------------------------------------------------

    private BeansConfig parseXml(String resource) {
        try {
            JAXBContext ctx = JAXBContext.newInstance(BeansConfig.class);
            Unmarshaller um = ctx.createUnmarshaller();
            InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
            if (is == null) {
                throw new BeanException("XML resource not found on classpath: " + resource);
            }
            return (BeansConfig) um.unmarshal(is);
        } catch (BeanException e) {
            throw e;
        } catch (Exception e) {
            throw new BeanException("Failed to parse XML config: " + resource, e);
        }
    }
}
