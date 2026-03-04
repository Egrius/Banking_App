package org.example.dto.error;

import jakarta.validation.ConstraintViolation;

public record ViolationDto(
        String field,
        String message,
        Object rejectedValue
) {
    public static ViolationDto fromViolation(ConstraintViolation<?> violation) {
        return new ViolationDto(
                violation.getPropertyPath().toString(),
                violation.getMessage(),
                violation.getInvalidValue()
        );
    }
}
