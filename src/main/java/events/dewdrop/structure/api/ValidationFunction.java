package events.dewdrop.structure.api;

import events.dewdrop.api.validators.ValidationException;

@FunctionalInterface
public interface ValidationFunction {

    void validate() throws ValidationException;
}

