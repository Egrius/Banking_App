package org.example.dto.transaction_template;

import org.example.dto.account.AccountSummaryDto;
import org.example.entity.enums.CurrencyCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionTemplateReadDto(
        String beneficiaryName,
        String beneficiaryAccountNumber,
        String beneficiaryBank,
        String beneficiaryBic,
        String beneficiaryInn,
        BigDecimal amount,
        CurrencyCode currencyCode,
        String description,
        AccountSummaryDto account,
        AccountSummaryDto defaultAccount,
        LocalDateTime createdAt,
        LocalDateTime lastUsedAt,
        Long usageCount
) { }
