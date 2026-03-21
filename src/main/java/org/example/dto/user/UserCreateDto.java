package org.example.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record UserCreateDto(

        @NotBlank(message = "Имя не может быть пустым")
        String firstName,

        @Length( max = 100, message = "Фамилия слишком длинная")
        @Length(min = 1, message = "Фамилия слишком короткая")
        String lastName,

        @NotBlank(message = "Пароль не может быть пустым")
        @Length(min = 5, max = 100, message = "Пароль должен быть от 5 до 100 символов")
        String rawPassword,

        @NotNull
        @Email
        String email
) { }