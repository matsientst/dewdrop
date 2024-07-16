package events.dewdrop.api.validators;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ValidationExceptionTest {
    @Test
    void validationException() {
        String message = "test";
        ValidationResult result = ValidationResult.of(new ValidationError(message));
        ValidationException exception = new ValidationException(result);

        assertEquals(message, exception.getValidationResult().get().get(0).getMessage());
    }

    @Test
    void getMessage() {
        ValidationResult result = ValidationResult.of("Something bad");
        result.and(ValidationResult.of("test"));
        ValidationException exception = new ValidationException(result);

        assertThat(exception.getMessage(), is("Something bad, test"));
        assertThat(new ValidationException(ValidationResult.valid()).getMessage(), is(""));
    }

    @Test
    void validationException_StringParam() {
        ValidationException exception = ValidationException.of("Something broke {}", "test");
        assertEquals("Something broke test", exception.getMessage());
    }

    @Test
    void of() {
        ValidationException exception = ValidationException.of("Something broke");
        assertEquals("Something broke", exception.getMessage());
    }

    @Test
    void of_exception() {
        ValidationException exception = ValidationException.of(new RuntimeException("Something broke"));
        assertEquals("Something broke", exception.getMessage());
    }

    @Test
    void of_invocationTargetException() {
        InvocationTargetException targetException = new InvocationTargetException(ValidationException.of("Something broke"));
        ValidationException exception = ValidationException.of(targetException);
        assertEquals("Something broke", exception.getMessage());
    }

    @Test
    void of_errors() {
        List<ValidationError> validationErrors = List.of(ValidationError.of("test"), ValidationError.of("test2"));
        ValidationException exception = ValidationException.of(validationErrors);
        assertEquals(exception.getValidationResult().get().get(0).getMessage(), validationErrors.get(0).getMessage());
        assertEquals(exception.getValidationResult().get().get(1).getMessage(), validationErrors.get(1).getMessage());
    }

    @Test
    void of_error() {
        ValidationError validationError = ValidationError.of("test");
        ValidationException exception = ValidationException.of(validationError);
        assertEquals(exception.getValidationResult().get().get(0).getMessage(), validationError.getMessage());
    }
}
