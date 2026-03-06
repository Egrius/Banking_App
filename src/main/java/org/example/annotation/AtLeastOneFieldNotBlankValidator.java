package org.example.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.example.dto.user.UserUpdateDto;

public class AtLeastOneFieldNotBlankValidator implements ConstraintValidator<AtLeastOneFieldNotBlank, UserUpdateDto> {
    @Override
    public boolean isValid(UserUpdateDto value, ConstraintValidatorContext context) {
        if(value == null) return false;

        return value.firstNameUpdated() != null && !value.firstNameUpdated().isBlank() ||
                value.lastNameUpdated() != null && !value.lastNameUpdated().isBlank();
    }
}
