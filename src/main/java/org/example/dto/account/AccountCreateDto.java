package org.example.dto.account;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import org.example.dto.RequestPayload;
import org.example.entity.enums.AccountType;
import org.example.entity.enums.CurrencyCode;

@JsonTypeName(value = "account.createAccount")
public record AccountCreateDto (
        @NotNull(message = "id пользователя обязателен для создании счёта")
        Long userId,

        @NotNull(message = "Тип валюты обязателен для создании счёта")
        CurrencyCode currencyCode,

        @NotNull(message = "Тип счёта обязателен")
        AccountType accountType
) implements RequestPayload { }
