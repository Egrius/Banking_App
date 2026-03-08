package org.example.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import java.util.Optional;


public abstract class BaseDaoImpl<T, ID> implements BaseDao<T, ID> {

    protected final Class<T> entityClass;

    public BaseDaoImpl(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public Optional<T> findById(EntityManager em, ID id) {
        return Optional.ofNullable(em.find(entityClass, id));
    }

    @Override
    public void save(EntityManager em, T entity) {
        em.persist(entity);
    }

    @Override
    public void update(EntityManager em, T entity) {
        em.merge(entity);
    }

    @Override
    public void delete(EntityManager em, T entity) {
        em.remove(entity);
    }

    public void deleteById(EntityManager em, ID id) {
        findById(em, id).ifPresent(entity -> delete(em, entity));
    }
}