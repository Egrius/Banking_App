package org.example.dto.account;

import org.example.entity.enums.AccountType;
import org.example.entity.enums.CurrencyCode;

public record AccountSummaryDto(
        Long id,
        String accountNumber,
        CurrencyCode currencyCode,
        AccountType accountType
) { }