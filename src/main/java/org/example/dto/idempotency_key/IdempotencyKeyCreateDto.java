package org.example.dto.idempotency_key;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.entity.Account;
import org.example.entity.BankTransaction;
import org.example.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record IdempotencyKeyCreateDto(
        @NotNull(message = "Ключ не может быть пустым")
        UUID key,

        @NotNull(message = "Транзакция должна быть передана для создания ключа в БД")
        BankTransaction transaction,

        @NotNull(message = "Время создания должна быть передана для создания ключа в БД")
        LocalDateTime createdAt
) { }
