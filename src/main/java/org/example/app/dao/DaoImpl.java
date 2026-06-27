package org.example.app.dao;

import org.example.framework.annotations.Component;

@Component("dao")
public class DaoImpl implements IDao {

    @Override
    public double getData() {
        System.out.println("[DaoImpl] Fetching data from database...");
        return 42.0;
    }
}
