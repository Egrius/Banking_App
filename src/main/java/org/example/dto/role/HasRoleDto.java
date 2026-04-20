package org.example.dto.role;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.example.dto.RequestPayload;

@JsonTypeName("role.hasRole")
public record HasRoleDto(
        @NotNull(message = "userId обязателен")
        @Positive(message = "userId должен быть положительным")
        Long userId,

        @NotBlank(message = "roleName не может быть пустым")
        String roleName
) implements RequestPayload {}