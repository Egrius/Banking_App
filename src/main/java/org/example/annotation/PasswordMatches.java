package org.example.annotation;

import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatches {
    String message() default "Новый пароль должен быть подтверждён";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
