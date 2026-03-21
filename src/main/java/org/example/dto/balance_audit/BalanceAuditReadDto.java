package org.example.dto.balance_audit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BalanceAuditReadDto (
       BigDecimal balanceBefore,
       BigDecimal balanceAfter,
       BigDecimal changeAmount,
       LocalDateTime changedAt,
       String changedByThread
){ }
