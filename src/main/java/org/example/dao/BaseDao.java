package org.example.dao;

import java.util.List;
import java.util.Optional;

public interface BaseDao <T, ID> {
    Optional<T> findById(ID id);
    void save(T entity);
    void update(T entity);
    void delete(T entity);
}