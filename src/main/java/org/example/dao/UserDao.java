package org.example.dao;

import jakarta.persistence.EntityManager;
import org.example.dto.request.PageRequest;
import org.example.dto.response.PageResponse;
import org.example.entity.User;

import java.util.List;
import java.util.Optional;

public class UserDao extends BaseDaoImpl<User, Long> {

    public UserDao() {
        super(User.class);
    }

    public Optional<User> findByEmail(EntityManager em, String email) {

        return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst();

    }

    public PageResponse<User> findAllPageable(EntityManager em, PageRequest pageRequest) {

        Long total = em.createQuery("SELECT COUNT(u) FROM User u", Long.class)
                .getSingleResult();


        List<User> users = em.createQuery("SELECT u FROM User u", User.class)
                .setFirstResult(pageRequest.getPageNumber() * pageRequest.getPageSize())
                .setMaxResults(pageRequest.getPageSize())
                .getResultList();

        return new PageResponse<>(users, pageRequest.getPageNumber(), pageRequest.getPageSize(), total);

    }

}