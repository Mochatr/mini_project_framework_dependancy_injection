package org.example.app.metier;

import org.example.app.dao.IDao;
import org.example.framework.annotations.Autowired;
import org.example.framework.annotations.Component;
import org.example.framework.annotations.Value;

/**
 * Business-layer bean — demonstrates <b>field injection</b>.
 * The container sets the field directly via reflection after instantiation.
 */
@Component("metierField")
public class MetierImplField implements IMetier {

    /** Field injection point (bean reference). */
    @Autowired
    private IDao dao;

    /** Field injection point (literal value). */
    @Value("10")
    private int multiplier;

    @Override
    public double calcul() {
        System.out.println("[MetierImplField] Performing business calculation (field injection, multiplier=" + multiplier + ")...");
        return dao.getData() * multiplier;
    }
}
