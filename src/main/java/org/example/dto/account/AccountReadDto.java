package org.example.dto.account;

import org.example.dto.user.UserReadDto;
import org.example.entity.enums.AccountType;
import org.example.entity.enums.CurrencyCode;
import org.example.entity.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountReadDto(
        Long id,
        UserReadDto user,
        String accountNumber,
        BigDecimal balance,
        CurrencyCode currencyCode,
        AccountType accountType,
        Status status,
        LocalDateTime openingDate,
        LocalDateTime closingDate
) { }
