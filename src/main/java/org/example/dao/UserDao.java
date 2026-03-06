package org.example.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.entity.User;

import java.util.List;
import java.util.Optional;

public class UserDao extends BaseDaoImpl<User, Long> {

    public UserDao(EntityManagerFactory entityManagerFactory) {
        super(User.class, entityManagerFactory);
    }

    public Optional<User> findByEmail(String email) {
        EntityManager em = getEntityManager();

        try {
            return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getResultStream()
                    .findFirst();
        } finally {
            if(em.isOpen()) em.close();
        }
    }

    public List<User> findAll() {
        EntityManager em = getEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u", User.class)
                    .getResultList();
        } finally {
            if(em.isOpen()) em.close();
        }
    }

}