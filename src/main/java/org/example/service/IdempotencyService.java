package org.example.service;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.IdempotencyKeyDao;
import org.example.dto.idempotency_key.IdempotencyKeyReadDto;
import org.example.entity.IdempotencyKey;
import org.example.mapper.IdempotencyKeyReadMapper;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {

    private final EntityManagerFactory emf;
    private final IdempotencyKeyDao keyDao;
    private final IdempotencyKeyReadMapper keyReadMapper;

    public Optional<IdempotencyKeyReadDto> getKey(UUID key) {
        try(EntityManager em = emf.createEntityManager();) {

            IdempotencyKey idempotencyKey = keyDao.findByKeySignature(em, key);

            if(idempotencyKey != null) {
                return Optional.of(keyReadMapper.map(idempotencyKey));
            }
            return Optional.empty();
        }

    }

    public boolean keyExistsBySignature(UUID key) {
        try(EntityManager em = emf.createEntityManager();) {
            return keyDao.keyExistsBySignature(em, key);
        }
    }

}
