package org.example.app.metier;

import org.example.app.dao.IDao;
import org.example.framework.annotations.Autowired;
import org.example.framework.annotations.Component;

/**
 * Business-layer bean — demonstrates <b>setter injection</b>.
 * The container calls {@link #setDao(IDao)} after instantiation.
 */
@Component("metierSetter")
public class MetierImplSetter implements IMetier {

    private IDao dao;

    /** Setter injection point. */
    @Autowired
    public void setDao(IDao dao) {
        this.dao = dao;
    }

    @Override
    public double calcul() {
        System.out.println("[MetierImplSetter] Performing business calculation (setter injection)...");
        return dao.getData() * 2;
    }
}
