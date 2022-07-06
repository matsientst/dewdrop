package org.dewdrop.structure.api.validator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.Validate;
import org.dewdrop.api.validators.ValidationError;
import org.dewdrop.api.validators.ValidationException;
import org.dewdrop.api.validators.ValidationResult;
import org.dewdrop.fixture.command.DewdropCreateUserCommand;
import org.junit.jupiter.api.Test;

class DewdropValidatorTest {
    @Test
    void invalid() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(null, null);

        try {
            DewdropValidator.withRule(() -> Validate.notBlank(command.getUsername(), "username is required")).andRule(() -> Validate.notNull(command.getUserId(), "userId is required")).validate();
        } catch (ValidationException e) {
            ValidationResult validationResult = e.getValidationResult();
            List<ValidationError> validationErrors = validationResult.get();
            assertThat(validationErrors.size(), is(2));
            assertThat(validationErrors.get(0).getMessage(), is("username is required"));
            assertThat(validationErrors.get(1).getMessage(), is("userId is required"));
        }
    }

    @Test
    void valid() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "Test");
        try {
            DewdropValidator.withRule(() -> Validate.notBlank(command.getUsername(), "username is required")).andRule(() -> Validate.notNull(command.getUserId(), "userId is required")).validate();
        } catch (ValidationException e) {
            fail("Should not throw exception");
        }
    }

    @Test
    void jsr303_valid() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(UUID.randomUUID(), "Test");
        try {
            DewdropValidator.validate(command);
        } catch (ValidationException e) {
            fail("Should not throw exception");
        }
    }

    @Test
    void jsr303_invalid() {
        DewdropCreateUserCommand command = new DewdropCreateUserCommand(null, null);
        try {
            DewdropValidator.validate(command);
        } catch (ValidationException e) {
            ValidationResult validationResult = e.getValidationResult();
            List<ValidationError> validationErrors = validationResult.get();
            assertThat(validationErrors.size(), is(2));

            assertThat(validationErrors.stream().filter(error -> error.getMessage().equals("UserId is required")).findAny().get().getMessage(), is("UserId is required"));
            assertThat(validationErrors.stream().filter(error -> error.getMessage().equals("Username is required")).findAny().get().getMessage(), is("Username is required"));
        }
    }
}
