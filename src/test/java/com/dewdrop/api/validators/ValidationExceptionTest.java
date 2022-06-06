package com.dewdrop.api.validators;

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

}
