package org.example.dto.idempotency_key;

import java.time.LocalDateTime;
import java.util.UUID;

public record IdempotencyKeyReadDto (
        Long id,
        UUID key,
        Long transactionId,
        Long accountId,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) { }
