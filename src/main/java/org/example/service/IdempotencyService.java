package org.example.service;

import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.IdempotencyKeyDao;
import org.example.dto.idempotency_key.IdempotencyKeyCreateDto;
import org.example.dto.idempotency_key.IdempotencyKeyReadDto;
import org.example.entity.IdempotencyKey;
import org.example.mapper.IdempotencyKeyCreateMapper;
import org.example.mapper.IdempotencyKeyReadMapper;
import org.example.util.ValidatorUtil;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class IdempotencyService {

    private final EntityManagerFactory emf;
    private final IdempotencyKeyDao keyDao;
    private final IdempotencyKeyReadMapper keyReadMapper;
    private final IdempotencyKeyCreateMapper keyCreateMapper;

    private static final ScheduledExecutorService scheduledCleaner = Executors.newSingleThreadScheduledExecutor();


    public IdempotencyService(EntityManagerFactory emf, IdempotencyKeyDao keyDao, IdempotencyKeyReadMapper keyReadMapper, IdempotencyKeyCreateMapper keyCreateMapper) {
        this.emf = emf;
        this.keyDao = keyDao;
        this.keyReadMapper = keyReadMapper;
        this.keyCreateMapper = keyCreateMapper;

    }

    private Runnable assignCleanTask(EntityManagerFactory emf) {
        return () -> {
            EntityManager em = emf.createEntityManager();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                keyDao.deleteAllExpired(em);
                tx.commit();
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
            } finally {
                em.close();
            }
        };

    }

    public static void close() {
        scheduledCleaner.shutdown();

        try{
            if(!scheduledCleaner.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduledCleaner.shutdown();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<IdempotencyKeyReadDto> getKey(UUID key) {
        try(EntityManager em = emf.createEntityManager();) {

            IdempotencyKey idempotencyKey = keyDao.findByKeySignature(em, key);

            if(idempotencyKey != null) {
                return Optional.of(keyReadMapper.map(idempotencyKey));
            }
            return Optional.empty();
        }

    }

    public void createKey(IdempotencyKeyCreateDto createDto, EntityManager em) {

        ValidatorUtil.validate(createDto);

        try {

            IdempotencyKey keyToCreate = keyCreateMapper.map(createDto);

            keyToCreate.setExpiresAt(keyToCreate.getCreatedAt().plusMinutes(45L));

            keyDao.save(em, keyToCreate);

            log.info("Ключ идемпотентности для транзакции c id {} успешно создан и сохранён в БД",createDto.transaction().getId());
        } catch (Exception e) {
            log.error("Ошибка при создании записи ключа в БД: {}", e.getMessage());
            throw e;
        }
    }

    public boolean keyExistsBySignature(UUID key) {
        try(EntityManager em = emf.createEntityManager();) {
            return keyDao.keyExistsBySignature(em, key);
        }
    }

    public void delete(Long id) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            IdempotencyKey key = keyDao.findById(em, id)
                    .orElseThrow(() -> new EntityNotFoundException("Ошибка: ключ идемпотентности с id " + id + " не найден"));

            keyDao.delete(em, key);
            tx.commit();

        } catch (Exception e) {
            log.error(e.getMessage());
            if(tx.isActive()) tx.rollback();
        } finally {
            em.close();
        }
    }
}
