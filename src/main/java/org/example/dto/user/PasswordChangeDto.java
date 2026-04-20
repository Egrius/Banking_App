package org.example.dto.user;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotBlank;
import org.example.dto.RequestPayload;
import org.hibernate.validator.constraints.Length;


@JsonTypeName(value = "user.changePassword")
public record PasswordChangeDto(
        @NotBlank(message = "Старый пароль обязателен")
        String oldPassword,

        @NotBlank(message = "Новый пароль обязателен")
        @Length(min = 5, max = 100, message = "Пароль должен быть от 5 до 100 символов")
        String newPassword,

        @NotBlank(message = "Подтверждение пароля обязательно")
        String confirmPassword
) implements RequestPayload {}