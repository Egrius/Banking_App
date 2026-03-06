package org.example.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AtLeastOneFieldNotBlankValidator.class)
public @interface AtLeastOneFieldNotBlank {
    String message() default "Хотя бы одно поле для обновления должно быть заполнено";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
