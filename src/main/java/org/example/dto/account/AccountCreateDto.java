package org.example.dto.account;

import jakarta.validation.constraints.NotNull;
import org.example.entity.enums.AccountType;
import org.example.entity.enums.CurrencyCode;

public record AccountCreateDto(
        @NotNull(message = "id пользователя обязателен для создании счёта")
        Long userId,

        @NotNull(message = "Тип валюты обязателен для создании счёта")
        CurrencyCode currencyCode,

        @NotNull(message = "Тип счёта обязателен")
        AccountType accountType
) { }
