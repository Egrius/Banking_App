package org.example.dto.role;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.example.dto.RequestPayload;

import java.util.Set;

@JsonTypeName("role.hasAnyRole")
public record HasAnyRoleDto(
        @NotNull(message = "userId обязателен")
        @Positive(message = "userId должен быть положительным")
        Long userId,

        @NotNull(message = "roleNames обязателен")
        Set<@NotBlank String> roleNames
) implements RequestPayload {}