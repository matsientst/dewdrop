package events.dewdrop.api.validators;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
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

    public static ValidationException of(ValidationError error) {
        return new ValidationException(ValidationResult.of(error));
    }

    public static ValidationException of(List<ValidationError> errors) {
        return new ValidationException(ValidationResult.of(errors));
    }

    public static ValidationException of(Exception exception) {
        if (exception instanceof ValidationException) { return (ValidationException) exception; }
        if (exception instanceof InvocationTargetException && ((InvocationTargetException) exception).getTargetException() instanceof ValidationException) { return (ValidationException) ((InvocationTargetException) exception).getTargetException(); }
        return ValidationException.of(exception.getMessage());
    }

    public static ValidationException of(String message, Object... params) {
        message = message.replaceAll(Pattern.quote("{}"), "%s");
        return new ValidationException(ValidationResult.of(new ValidationError(message, params)));
    }

}
