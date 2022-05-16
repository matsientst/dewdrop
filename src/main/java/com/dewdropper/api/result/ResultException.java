package com.dewdropper.api.result;

public class ResultException extends Exception {
    public ResultException(Exception e) {
        super(e);
    }

    private ResultException() {}
}
