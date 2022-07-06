package org.dewdrop.api.validators;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Data;

/**
 * An exception used to throw an exception with a ValidationResult which contains a collection of
 * ValidationErrors. These can then be used to understand all errors in the Validation not just the
 * first one.
 */
@Data
public class ValidationException extends Exception {
    private final ValidationResult validationResult;

    public ValidationException(ValidationResult result) {
        super(result.get().stream().map(ValidationError::getMessage).collect(Collectors.joining(", ")));
        this.validationResult = result;
    }

    public static ValidationException of(String message) {
        return new ValidationException(ValidationResult.of(new ValidationError(message)));
    }

    public static ValidationException of(Exception exception) {
        if (exception instanceof ValidationException) { return (ValidationException) exception; }
        return ValidationException.of(exception.getMessage());
    }

    public static ValidationException of(String message, Object... params) {
        message = message.replaceAll(Pattern.quote("{}"), "%s");
        return new ValidationException(ValidationResult.of(new ValidationError(message, params)));
    }

}
