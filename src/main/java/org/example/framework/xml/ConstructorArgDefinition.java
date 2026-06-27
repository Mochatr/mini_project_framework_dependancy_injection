package org.example.framework.xml;

import jakarta.xml.bind.annotation.*;

/**
 * JAXB model for a &lt;constructor-arg&gt; element.
 * Either {@code ref} (bean name) or {@code value} (literal) must be set.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ConstructorArgDefinition {

    @XmlAttribute
    private String ref;

    @XmlAttribute
    private String value;

    @XmlAttribute
    private String type;

    public String getRef()   { return ref; }
    public String getValue() { return value; }
    public String getType()  { return type; }

    public void setRef(String ref)     { this.ref = ref; }
    public void setValue(String value) { this.value = value; }
    public void setType(String type)   { this.type = type; }
}
