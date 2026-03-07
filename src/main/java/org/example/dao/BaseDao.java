package org.example.dao;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public interface BaseDao <T, ID> {

    Optional<T> findById(ID id, EntityManager em);

    void save(T entity, EntityManager em);

    void update(T entity, EntityManager em);

    void delete(T entity, EntityManager em);
}