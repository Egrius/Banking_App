package org.example.dto.user;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotBlank;
import org.example.annotation.AtLeastOneFieldNotBlank;
import org.example.dto.RequestPayload;
import org.hibernate.validator.constraints.Length;

@AtLeastOneFieldNotBlank
@JsonTypeName(value = "user.updateUser")
public record UserUpdateDto(
        @Length(max = 100, message = "Имя слишком длинное")
        @Length(min = 1, message = "Имя слишком короткое")
        String firstNameUpdated,

        @Length(max = 100, message = "Фамилия слишком длинная")
        @Length(min = 1, message = "Фамилия слишком короткая")
        String lastNameUpdated
) implements RequestPayload { }
