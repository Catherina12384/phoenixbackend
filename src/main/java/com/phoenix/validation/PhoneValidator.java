package com.phoenix.validation;

import com.phoenix.common.PhoneNumbers;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        return value == null || PhoneNumbers.tryNormalize(value).isPresent();   // pair with @NotBlank when required
    }
}
