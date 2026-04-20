package org.example.dto.user;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.example.dto.RequestPayload;

@JsonTypeName(value = "user.login")
public record UserLoginDto(
        @NotBlank(message = "email не может быть пустым")
        @Email(message = "email введён некорректно")
        String email,

        @NotBlank(message = "пароль не должен быть пустым")
        String rawPassword
) implements RequestPayload { }
