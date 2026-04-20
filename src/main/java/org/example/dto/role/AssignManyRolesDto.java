package org.example.dto.role;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.example.dto.RequestPayload;

import java.util.Set;

@JsonTypeName("role.assignMany")
public record AssignManyRolesDto(
        @NotNull(message = "userId обязателен")
        @Positive(message = "userId должен быть положительным")
        Long userId,

        @NotNull(message = "roleIds обязателен")
        Set<@Positive Long> roleIds
) implements RequestPayload {}