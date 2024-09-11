package events.dewdrop.api.validators;

import lombok.Data;

import java.io.Serializable;

@Data
public class ValidationError implements Serializable {
    String field;
    String message;

    public ValidationError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public ValidationError(String message) {
        this.message = message;
    }

    public ValidationError(String message, Object... params) {
        this.message = String.format(message, params);
    }

    public ValidationError(String field, String message, Object... params) {
        this.field = field;
        this.message = String.format(message, params);
    }

    public static ValidationError of(String message) {
        return new ValidationError(message);
    }

    public static ValidationError of(String field, String message) {
        return new ValidationError(field, message);
    }
}
