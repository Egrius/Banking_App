package org.example.dto.role;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.example.dto.RequestPayload;
import org.hibernate.validator.constraints.Length;

@JsonTypeName("role.updateRole")
public record RoleUpdateDto (

        @NotNull(message = "Параметр roleId не может быть пустым")
        @Positive(message = "Параметр roleId не может принимать отрицательных значений")
        Long roleId,

        @NotBlank(message = "Параметр newRoleName не может быть пустым или null")
        @Length(min = 3, max = 10, message = "Название роли должно быть в пределах от  3 до 10 символов")
        String newRoleName
) implements RequestPayload { }
