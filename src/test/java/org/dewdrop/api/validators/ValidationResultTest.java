package org.dewdrop.api.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidationResultTest {
    String message = "test error message";

    @Test
    void addAll() {
        List<ValidationError> errors = List.of(new ValidationError(message));
        ValidationResult result = ValidationResult.valid();
        result.addAll(errors);

        assertTrue(result.hasErrors());
        assertEquals(message, result.get().get(0).getMessage());
    }

    @Test
    void of_withErrors() {
        List<ValidationError> errors = List.of(new ValidationError(message));
        ValidationResult result = ValidationResult.of(errors);

        assertTrue(result.hasErrors());
        assertEquals(message, result.get().get(0).getMessage());
    }

    @Test
    void of_noErrors() {
        List<ValidationError> errors = new ArrayList<>();
        ValidationResult result = ValidationResult.of(errors);

        assertFalse(result.hasErrors());
    }

    @Test
    void of_withSingleError() {
        ValidationResult result = ValidationResult.of(new ValidationError(message));

        assertTrue(result.hasErrors());
        assertEquals(message, result.get().get(0).getMessage());
    }

    @Test
    void valid() {
        ValidationResult result = ValidationResult.valid();

        assertFalse(result.hasErrors());
    }

    @Test
    void get() {
        ValidationResult result = ValidationResult.of(new ValidationError(message));

        assertTrue(result.hasErrors());
        assertEquals(message, result.get().get(0).getMessage());
    }

    @Test
    void add() {
        String nextMessage = "next error message";
        ValidationResult result = ValidationResult.of(new ValidationError(message));
        result.add(new ValidationError(nextMessage));

        assertTrue(result.hasErrors());
        assertEquals(message, result.get().get(0).getMessage());
        assertEquals(nextMessage, result.get().get(1).getMessage());
    }

    @Test
    void and() {
        String nextMessage = "next error message";
        ValidationResult result = ValidationResult.of(new ValidationError(message));
        result.and(ValidationResult.of(new ValidationError(nextMessage)));
        result.and(ValidationResult.valid());

        assertTrue(result.hasErrors());
        assertEquals(message, result.get().get(0).getMessage());
        assertEquals(nextMessage, result.get().get(1).getMessage());
    }

    @Test
    void and_noErrorsYet() {
        ValidationResult result = ValidationResult.valid();
        result.and(ValidationResult.of(new ValidationError(message)));

        assertTrue(result.hasErrors());
        assertEquals(message, result.get().get(0).getMessage());
    }

    @Test
    void isValid() {
        ValidationResult result = ValidationResult.valid();
        assertTrue(result.isValid());
    }

    @Test
    void isValid_butItIsnt() {
        ValidationResult result = ValidationResult.of(new ValidationError(message));
        assertFalse(result.isValid());
    }

}
