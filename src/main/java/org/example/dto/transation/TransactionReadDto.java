package org.example.dto.transation;

import org.example.dto.account.AccountReadDto;
import org.example.entity.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionReadDto(
        Long id,
        AccountReadDto fromAccount,
        AccountReadDto toAccount,
        BigDecimal amount,
        TransactionStatus status,
        String failureReason,
        Integer retryCount,
        BigDecimal fromBalanceBefore,
        BigDecimal fromBalanceAfter,
        BigDecimal toBalanceBefore,
        BigDecimal toBalanceAfter,
        LocalDateTime createdAt,
        LocalDateTime processedAt,
        String description
) { }
