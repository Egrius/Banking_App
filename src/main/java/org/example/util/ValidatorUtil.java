package org.example.util;

import jakarta.validation.*;
import org.example.dto.error.ViolationDto;
import org.example.dto.response.ValidationErrorResponse;
import org.example.exception.CustomValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ValidatorUtil {

    private static final ValidatorFactory factory =
            Validation.buildDefaultValidatorFactory();

    private static final Validator validator =
            factory.getValidator();

    public static <T> void validate(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);

        if(!violations.isEmpty()) {

            List<ViolationDto> violationDtos = new ArrayList<>();
            for (ConstraintViolation<T> v : violations) {

                violationDtos.add(ViolationDto.fromViolation(v));
            }
            ValidationErrorResponse errorResponse = new ValidationErrorResponse(violationDtos);

            throw new CustomValidationException(errorResponse);
        }
    }

    public static void close() {
        factory.close();
    }
}
