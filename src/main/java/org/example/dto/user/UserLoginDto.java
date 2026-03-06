package org.example.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserLoginDto(
        @NotBlank(message = "email не может быть пустым")
        @Email(message = "email введён некорректно")
        String email,

        @NotBlank(message = "пароль не должен быть пустым")
        String rawPassword
) { }
