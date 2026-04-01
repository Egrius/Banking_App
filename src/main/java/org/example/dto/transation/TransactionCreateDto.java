package org.example.dto.transation;

import jakarta.validation.constraints.NotNull;
import org.example.entity.enums.CurrencyCode;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionCreateDto(
        @NotNull(message = "Идентификатор аккаунта-отправителя не может быть пустым")
        Long fromAccountId,

        @NotNull(message = "Идентификатор аккаунта-получателя не может быть пустым")
        Long toAccountId,

        @NotNull(message = "Тип валюты обязателен")
        CurrencyCode currencyCode,

        @NotNull(message = "Сумма не может быть пустой")
        BigDecimal amount,

        @Length(max = 200, message = "Размер описания не должен превышать 200 символов")
        String description,

        @NotNull(message = "Ключ идемпотентности не может быть пустым")
        UUID idempotencyKey
) { }
