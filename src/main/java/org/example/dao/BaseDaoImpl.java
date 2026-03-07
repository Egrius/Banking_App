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

    // Все методы принимают EntityManager извне
    @Override
    public Optional<T> findById(ID id, EntityManager em) {
        return Optional.ofNullable(em.find(entityClass, id));
    }

    @Override
    public void save(T entity, EntityManager em) {
        // НЕТ управления транзакцией! Только операция
        em.persist(entity);
    }

    @Override
    public void update(T entity, EntityManager em) {
        // НЕТ управления транзакцией! Только операция
        em.merge(entity);
    }

    @Override
    public void delete(T entity, EntityManager em) {
        // НЕТ управления транзакцией! Только операция
        em.remove(entity);
    }

    // Дополнительный удобный метод
    public void deleteById(ID id, EntityManager em) {
        findById(id, em).ifPresent(entity -> delete(entity, em));
    }
}