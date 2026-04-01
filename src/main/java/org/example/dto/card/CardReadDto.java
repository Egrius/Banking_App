package org.example.dto.card;

import org.example.entity.enums.CardStatus;
import org.example.entity.enums.CardType;
import org.example.entity.enums.CurrencyCode;

public record CardReadDto(
        Long id,
        Long userId,
        String userName,
        Long accountId,
        String accountNumber,
        String cardNumber,
        CurrencyCode currencyCode,
        String cardholderName,
        String expiryDate,
        CardType cardType,
        CardStatus status,
        String name,
        boolean isExpired
) {}