package org.example.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import org.example.entity.Role;
import org.example.entity.User;
import org.hibernate.SessionFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RoleDao extends BaseDaoImpl<Role, Long> {

    public RoleDao() {
        super(Role.class);
    }

    public Optional<Role> findByName(EntityManager em,
                                     String name) {

        try {
            return Optional.of( em.createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class)
                    .setParameter("name", name)
                    .getSingleResult());

        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<Role> findAll(EntityManager em) {
        try{
            return em.createQuery("SELECT r FROM Role r", Role.class)
                    .getResultList();
        } catch (NoResultException e) {
            return Collections.emptyList();
        }
    }

    public List<User> findUsersByRoleId(Long roleId, EntityManager em) {
        return em.createQuery("SELECT u FROM User u JOIN u.roles r WHERE r.id = :roleId", User.class)
                .setParameter("roleId", roleId)
                .getResultList();

    }

}
