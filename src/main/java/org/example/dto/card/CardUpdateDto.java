package org.example.dto.card;

import jakarta.validation.constraints.Size;

public record CardUpdateDto(
        @Size(max = 50, message = "Название карты не должно превышать 50 символов")
        String name
) {}