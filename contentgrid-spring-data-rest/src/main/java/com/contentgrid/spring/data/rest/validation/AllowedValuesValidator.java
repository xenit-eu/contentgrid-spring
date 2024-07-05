package com.contentgrid.spring.data.rest.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

public class AllowedValuesValidator implements ConstraintValidator<AllowedValues, String> {

    private Set<String> options;

    @Override
    public void initialize(AllowedValues constraintAnnotation) {
        this.options = Set.of(constraintAnnotation.options());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            // Null values are checked by other constraints
            return true;
        }
        return options.contains(value);
    }
}
