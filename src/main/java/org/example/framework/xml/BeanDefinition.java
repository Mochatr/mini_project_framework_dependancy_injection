package org.example.framework.xml;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JAXB model for a &lt;bean&gt; element.
 *
 * <pre>{@code
 * <bean id="dao" class="org.example.app.dao.DaoImpl"/>
 *
 * <bean id="metier" class="org.example.app.metier.MetierImpl">
 *     <property name="dao" ref="dao"/>
 * </bean>
 *
 * <bean id="metier2" class="org.example.app.metier.MetierImpl2">
 *     <constructor-arg ref="dao"/>
 * </bean>
 * }</pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BeanDefinition {

    @XmlAttribute(required = true)
    private String id;

    @XmlAttribute(name = "class", required = true)
    private String className;

    @XmlElement(name = "property")
    private List<PropertyDefinition> properties = new ArrayList<>();

    @XmlElement(name = "constructor-arg")
    private List<ConstructorArgDefinition> constructorArgs = new ArrayList<>();

    public String getId()        { return id; }
    public String getClassName() { return className; }

    public List<PropertyDefinition>       getProperties()      { return properties; }
    public List<ConstructorArgDefinition> getConstructorArgs() { return constructorArgs; }

    public void setId(String id)              { this.id = id; }
    public void setClassName(String className){ this.className = className; }
}
