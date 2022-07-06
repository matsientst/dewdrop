package org.dewdrop.structure.api.validator;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.dewdrop.api.validators.ValidationError;
import org.dewdrop.api.validators.ValidationException;
import org.dewdrop.api.validators.ValidationResult;
import org.dewdrop.structure.api.Command;
import org.dewdrop.structure.api.ValidationFunction;

public class DewdropValidator {
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();
    private List<ValidationFunction> validations = new ArrayList<>();

    private DewdropValidator(ValidationFunction validationFunction) {
        validations.add(validationFunction);
    }

    public static DewdropValidator withRule(ValidationFunction validationFunction) {
        return new DewdropValidator(validationFunction);
    }

    public DewdropValidator andRule(ValidationFunction validationFunction) {
        validations.add(validationFunction);
        return this;
    }

    public void validate() throws ValidationException {
        ValidationResult results = ValidationResult.valid();
        for (ValidationFunction validationFunction : validations) {
            try {
                validationFunction.validate();
            } catch (Exception e) {
                results.add(ValidationError.of(e.getMessage()));
            }
        }
        if (results.hasErrors()) { throw new ValidationException(results); }
    }

    public static <T extends Command> void validate(T command) throws ValidationException {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(command);
        if (!violations.isEmpty()) {
            List<ValidationError> errors = violations.stream().map(ConstraintViolation::getMessage).distinct().map(ValidationError::of).collect(toList());
            ValidationResult result = ValidationResult.of(errors);
            throw new ValidationException(result);
        }
    }
}
