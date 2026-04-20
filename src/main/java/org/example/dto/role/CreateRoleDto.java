package org.example.dto.role;

import jakarta.validation.constraints.NotBlank;
import org.example.dto.RequestPayload;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.hibernate.validator.constraints.Length;

@JsonTypeName("role.create")
public record CreateRoleDto(
        @NotBlank(message = "Название роли не может быть пустым")
        @Length(min = 3, max = 10, message = "Название роли должно быть в пределах от  3 до 10 символов")
        String roleName
) implements RequestPayload {}