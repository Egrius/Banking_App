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

    public RoleDao(SessionFactory sessionFactory) {
        super(Role.class, sessionFactory);
    }

    public Optional<Role> findByName(String name) {
        EntityManager em = getEntityManager();

        try {
            return Optional.of( em.createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class)
                    .setParameter("name", name)
                    .getSingleResult());

        } catch (NoResultException e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }

    public List<Role> findAll() {
        EntityManager em = getEntityManager();
        try{
            return em.createQuery("SELECT r FROM Role r", Role.class)
                    .getResultList();
        } catch (NoResultException e) {
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public List<User> findUsersByRoleId(Long roleId) {
        EntityManager em = getEntityManager();
        return em.createQuery("SELECT u.user_id from user_roles u WHERE u.role_id = :roleId")
    }

}
