package org.example.dto.role;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.example.dto.RequestPayload;

@JsonTypeName("role.assign")
public record AssignRoleDto(
        @NotNull(message = "userId обязателен")
        @Positive(message = "userId должен быть положительным")
        Long userId,

        @NotNull(message = "roleId обязателен")
        @Positive(message = "roleId должен быть положительным")
        Long roleId
) implements RequestPayload {}