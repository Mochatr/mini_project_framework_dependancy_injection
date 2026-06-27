package org.example.app.metier;

import org.example.app.dao.IDao;
import org.example.framework.annotations.Autowired;
import org.example.framework.annotations.Component;

/**
 * Business-layer bean — demonstrates <b>constructor injection</b>.
 * The container resolves {@link IDao} and passes it at construction time.
 */
@Component("metierConstructor")
public class MetierImplConstructor implements IMetier {

    private final IDao dao;

    /** Constructor injection point. */
    @Autowired
    public MetierImplConstructor(IDao dao) {
        this.dao = dao;
    }

    @Override
    public double calcul() {
        System.out.println("[MetierImplConstructor] Performing business calculation (constructor injection)...");
        return dao.getData() * 3;
    }
}
