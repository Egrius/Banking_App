package org.example.dto.user;

import jakarta.validation.constraints.NotBlank;
import org.example.annotation.PasswordMatches;
import org.hibernate.validator.constraints.Length;

@PasswordMatches
public record PasswordChangeDto(
        @NotBlank(message = "Старый пароль обязателен")
        String oldPassword,

        @NotBlank(message = "Новый пароль обязателен")
        @Length(min = 5, max = 100, message = "Пароль должен быть от 5 до 100 символов")
        String newPassword,

        @NotBlank(message = "Подтверждение пароля обязательно")
        String confirmPassword
) {}