package org.example.dto.card;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.Size;
import org.example.dto.RequestPayload;

@JsonTypeName(value = "card.updateCard")
public record CardUpdateDto(
        @Size(max = 50, message = "Название карты не должно превышать 50 символов")
        String name
) implements RequestPayload {}