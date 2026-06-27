package org.example.framework.xml;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JAXB root element: maps to &lt;beans&gt; in the XML configuration file.
 */
@XmlRootElement(name = "beans")
@XmlAccessorType(XmlAccessType.FIELD)
public class BeansConfig {

    @XmlElement(name = "bean")
    private List<BeanDefinition> beans = new ArrayList<>();

    public List<BeanDefinition> getBeans() { return beans; }
}
