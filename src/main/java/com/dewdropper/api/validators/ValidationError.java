package com.dewdropper.api.validators;

import java.io.Serializable;
import lombok.Data;

@Data
public class ValidationError implements Serializable {
    String message;

    public ValidationError(String message) {
        this.message = message;
    }

    public ValidationError(String message, Object... params) {
        this.message = String.format(message, params);
    }
}
