package org.example.dto.response;

import lombok.Value;
import org.example.dto.error.ViolationDto;

import java.util.List;

@Value
public class ValidationErrorResponse {
    String message = "Ошибки валидации";
    List<ViolationDto> violations;

    public ValidationErrorResponse(List<ViolationDto> violations) {
        this.violations = violations;
    }

    public boolean hasErrors() {
        return violations != null && !violations.isEmpty();
    }


}
