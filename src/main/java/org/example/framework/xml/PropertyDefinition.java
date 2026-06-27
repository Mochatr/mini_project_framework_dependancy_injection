package org.example.framework.xml;

import jakarta.xml.bind.annotation.*;

/**
 * JAXB model for a &lt;property&gt; element inside a &lt;bean&gt;.
 * Either {@code ref} (bean name) or {@code value} (literal string) must be set.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PropertyDefinition {

    @XmlAttribute(required = true)
    private String name;

    /** Reference to another bean by id. */
    @XmlAttribute
    private String ref;

    /** Literal primitive / String value. */
    @XmlAttribute
    private String value;

    public String getName()  { return name; }
    public String getRef()   { return ref; }
    public String getValue() { return value; }

    public void setName(String name)   { this.name = name; }
    public void setRef(String ref)     { this.ref = ref; }
    public void setValue(String value) { this.value = value; }
}
