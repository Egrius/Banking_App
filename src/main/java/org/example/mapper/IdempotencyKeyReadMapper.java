package org.example.mapper;

import org.example.dto.idempotency_key.IdempotencyKeyReadDto;
import org.example.entity.IdempotencyKey;

public class IdempotencyKeyReadMapper implements BaseMapper<IdempotencyKey, IdempotencyKeyReadDto> {
    @Override
    public IdempotencyKeyReadDto map(IdempotencyKey object) {
        return new IdempotencyKeyReadDto(object.getKey(),
                object.getTransaction().getId(),
                object.getAccount().getId(),
                object.getUser().getId(),
                object.getCreatedAt(),
                object.getExpiresAt()
        );
    }
}
