package org.example.service;

import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
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

@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {

    private final EntityManagerFactory emf;
    private final IdempotencyKeyDao keyDao;
    private final IdempotencyKeyReadMapper keyReadMapper;
    private final IdempotencyKeyCreateMapper keyCreateMapper;

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

        // EntityManager em = emf.createEntityManager();
       // EntityTransaction tx = em.getTransaction();

        try {

            IdempotencyKey keyToCreate = keyCreateMapper.map(createDto);

            keyToCreate.setExpiresAt(keyToCreate.getCreatedAt().plusMinutes(45L));

            keyDao.save(em, keyToCreate);

            log.info("Ключ идемпотентности для транзакции c id {} успешно создан и сохранён в БД",createDto.transaction().getId());
        } catch (Exception e) {
            log.error("Ошибка при создании записи ключа в БД: {}", e.getMessage());
            throw e;
            //if(tx.isActive()) tx.rollback();
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
