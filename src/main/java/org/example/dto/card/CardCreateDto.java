package org.example.dto.card;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.dto.RequestPayload;
import org.example.entity.enums.CardType;
import org.example.entity.enums.CurrencyCode;

@JsonTypeName(value = "card.createCard")
public record CardCreateDto(
        @NotNull(message = "ID счета не может быть пустым")
        Long accountId,

        @NotNull(message = "Валюта не может быть пустой")
        CurrencyCode currencyCode,

        @NotBlank(message = "Имя держателя не может быть пустым")
        String cardholderName,

        @NotNull(message = "Тип карты не может быть пустым")
        CardType cardType,

        String name
) implements RequestPayload {}
