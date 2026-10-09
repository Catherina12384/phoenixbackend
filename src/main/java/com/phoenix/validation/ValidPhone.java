package com.phoenix.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhone {
    String message() default "Enter a valid 10-digit Indian mobile number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
