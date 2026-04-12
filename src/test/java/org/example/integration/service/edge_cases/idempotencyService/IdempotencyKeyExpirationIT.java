package org.example.integration.service.edge_cases.idempotencyService;

import org.example.dao.IdempotencyKeyDao;
import org.example.integration.config.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class IdempotencyKeyExpirationIT extends AbstractIntegrationTest {

    private final IdempotencyKeyDao keyDao = new IdempotencyKeyDao();

    @Test
    void deleteAllExpired_shouldRemoveOnlyExpiredKeys() {
        try (EntityManager em = sessionFactory.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();


            // Вставляем свежий ключ
            em.createNativeQuery(
                            "INSERT INTO idempotency_keys (id, key, created_at, expires_at) " +
                                    "VALUES (nextval('idempotency_key_id_seq'), '123e4567-e89b-12d3-a456-426614174000', :now, :future)")
                    .setParameter("now", LocalDateTime.now())
                    .setParameter("future", LocalDateTime.now().plusHours(1))
                    .executeUpdate();

            // Вставляем просроченный ключ
            em.createNativeQuery(
                            "INSERT INTO idempotency_keys (id, key, created_at, expires_at) " +
                                    "VALUES (nextval('idempotency_key_id_seq'), '223e4567-e89b-12d3-a456-426614174001', :past, :pastExpired)")
                    .setParameter("past", LocalDateTime.now().minusHours(2))
                    .setParameter("pastExpired", LocalDateTime.now().minusHours(1))
                    .executeUpdate();

            tx.commit();

            EntityTransaction tx2 = em.getTransaction();
            tx2.begin();
            // Выполняем удаление
            int deleted = keyDao.deleteAllExpired(em);
            tx2.commit();
            assertEquals(1, deleted);

            // Проверяем, что свежий ключ остался

            Long count = em.createQuery(
                            "SELECT COUNT(ik) FROM IdempotencyKey ik WHERE ik.key = :key", Long.class)
                    .setParameter("key", UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                    .getSingleResult();
            assertEquals(1, count);

            // Просроченный удалён
            count = em.createQuery(
                            "SELECT COUNT(ik) FROM IdempotencyKey ik WHERE ik.key = :key", Long.class)
                    .setParameter("key", UUID.fromString("223e4567-e89b-12d3-a456-426614174001"))
                    .getSingleResult();
            assertEquals(0, count);
        }
    }
}