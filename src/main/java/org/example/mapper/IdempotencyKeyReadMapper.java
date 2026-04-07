package org.example.mapper;

import org.example.dto.idempotency_key.IdempotencyKeyReadDto;
import org.example.entity.IdempotencyKey;

public class IdempotencyKeyReadMapper implements BaseMapper<IdempotencyKey, IdempotencyKeyReadDto> {
    @Override
    public IdempotencyKeyReadDto map(IdempotencyKey object) {
        return new IdempotencyKeyReadDto(
                object.getId(),
                object.getKey(),
                object.getTransaction().getId(),
                object.getCreatedAt(),
                object.getExpiresAt()
        );
    }
}
