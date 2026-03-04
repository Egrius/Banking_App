package org.example.exception;

import jakarta.validation.ValidationException;
import org.example.dto.response.ValidationErrorResponse;

public class CustomValidationException extends ValidationException {

    private final transient ValidationErrorResponse errorResponse;

    public CustomValidationException(ValidationErrorResponse errorResponse) {
        super(errorResponse.getMessage());
        this.errorResponse = errorResponse;
    }

    public ValidationErrorResponse getErrorResponse() {
        return errorResponse;
    }
}
