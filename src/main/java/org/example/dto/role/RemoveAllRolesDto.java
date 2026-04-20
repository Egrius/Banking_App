package org.example.dto.role;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.example.dto.RequestPayload;

@JsonTypeName("role.removeAll")
public record RemoveAllRolesDto(
        @NotNull(message = "userId обязателен")
        @Positive(message = "userId должен быть положительным")
        Long userId
) implements RequestPayload {}