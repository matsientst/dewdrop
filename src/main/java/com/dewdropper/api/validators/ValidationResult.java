package com.dewdropper.api.validators;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ValidationResult implements Serializable {

    private final List<ValidationError> errors;

    protected ValidationResult() {
        this.errors = new ArrayList<>();
    }

    public static ValidationResult of(String message) {
        ValidationResult result = new ValidationResult();
        ValidationError error = new ValidationError(message);
        result.errors.add(error);
        return result;
    }

    public ValidationResult addAll(List<ValidationError> errors) {
        this.errors.addAll(errors);
        return this;
    }

    public static ValidationResult of(List<ValidationError> errors) {
        ValidationResult result = new ValidationResult();

        if (errors.isEmpty()) { return result; }

        result.addAll(errors);
        return result;
    }

    public static ValidationResult of(ValidationError error) {
        ValidationResult result = new ValidationResult();
        result.errors.add(error);
        return result;
    }

    public static ValidationResult valid() {
        return new ValidationResult();
    }

    public boolean hasErrors() {
        return !this.errors.isEmpty();
    }

    public boolean isValid() {
        return !hasErrors();
    }

    public List<ValidationError> get() {
        return this.errors;
    }

    public void add(ValidationError validationError) {
        this.errors.add(validationError);
    }

    public ValidationResult and(ValidationResult validationResult) {
        if (validationResult.hasErrors()) {
            this.errors.addAll(validationResult.get());
        }
        return this;
    }
}
