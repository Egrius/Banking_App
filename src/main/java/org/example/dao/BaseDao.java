package org.example.dao;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public interface BaseDao <T, ID> {

    Optional<T> findById(EntityManager em, ID id);

    void save(EntityManager em, T entity);

    void update(EntityManager em, T entity);

    void delete(EntityManager em, T entity);
}