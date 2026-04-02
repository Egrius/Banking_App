package org.example.dao;

import jakarta.persistence.EntityManager;
import org.example.entity.IdempotencyKey;

import java.util.Optional;
import java.util.UUID;

public class IdempotencyKeyDao extends BaseDaoImpl<IdempotencyKey, Long> {
    public IdempotencyKeyDao() {
        super(IdempotencyKey.class);
    }

    public IdempotencyKey findByKeySignature(EntityManager em, UUID key) {
        return em.createQuery("SELECT k FROM IdempotencyKey k WHERE k.key = :key", IdempotencyKey.class)
                .setParameter("key", key)
                .getSingleResult();
    }

    public boolean keyExistsBySignature(EntityManager em, UUID key) {
        return em.createQuery("SELECT 1 FROM IdempotencyKey k WHERE k.key = :key)", IdempotencyKey.class)
                .setParameter("key", key)
                .getResultList()
                .isEmpty();
    }

}
