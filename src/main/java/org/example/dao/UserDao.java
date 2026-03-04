package org.example.dao;

import jakarta.persistence.EntityManagerFactory;
import org.example.entity.User;

public class UserDao extends BaseDaoImpl<User, Long> {

    public UserDao(EntityManagerFactory entityManagerFactory) {
        super(User.class, entityManagerFactory);
    }

}