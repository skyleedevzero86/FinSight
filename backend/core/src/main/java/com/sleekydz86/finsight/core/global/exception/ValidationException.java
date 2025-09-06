package com.sleekydz86.finsight.core.global.exception;

import java.util.List;

public class ValidationException extends BaseException {
    private final List<String> validationErrors;

    public ValidationException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    public ValidationException(String message, String errorCode, List<String> validationErrors) {
        super(message, errorCode);
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}