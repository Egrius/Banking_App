package org.example.dto.user;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.dto.RequestPayload;
import org.hibernate.validator.constraints.Length;

@JsonTypeName(value = "user.register")
public record UserCreateDto(

        @NotBlank(message = "Имя не может быть пустым")
        String firstName,

        @Length( max = 100, message = "Фамилия слишком длинная")
        @Length(min = 1, message = "Фамилия слишком короткая")
        String lastName,

        @NotBlank(message = "Пароль не может быть пустым")
        @Length(min = 5, max = 100, message = "Пароль должен быть от 5 до 100 символов")
        String rawPassword,

        @NotBlank
        @Email
        String email
) implements RequestPayload { }