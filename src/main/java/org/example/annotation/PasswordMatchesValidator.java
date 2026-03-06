package org.example.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.example.dto.user.PasswordChangeDto;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, PasswordChangeDto> {
    @Override
    public boolean isValid(PasswordChangeDto value, ConstraintValidatorContext context) {
        return value.newPassword().equals(value.confirmPassword());
    }
}
