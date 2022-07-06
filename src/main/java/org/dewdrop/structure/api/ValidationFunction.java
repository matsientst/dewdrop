package org.dewdrop.structure.api;

import org.dewdrop.api.validators.ValidationException;

@FunctionalInterface
public interface ValidationFunction {

    void validate() throws ValidationException;
}

