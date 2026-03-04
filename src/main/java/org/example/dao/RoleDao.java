package org.example.dao;

import org.example.entity.User;
import org.hibernate.SessionFactory;

public class RoleDao extends BaseDaoImpl<User, Long> {

    public RoleDao(SessionFactory sessionFactory) {
        super(User.class, sessionFactory);
    }
}
