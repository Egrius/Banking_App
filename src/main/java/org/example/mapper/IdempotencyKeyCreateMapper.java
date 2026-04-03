package org.example.mapper;

import org.example.dto.idempotency_key.IdempotencyKeyCreateDto;
import org.example.entity.IdempotencyKey;

public class IdempotencyKeyCreateMapper implements BaseMapper<IdempotencyKeyCreateDto, IdempotencyKey> {
    @Override
    public IdempotencyKey map(IdempotencyKeyCreateDto object) {
        return IdempotencyKey.from(object.key(), object.transaction(), object.createdAt());
    }
}
