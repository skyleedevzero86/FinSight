package com.sleekydz86.finsight.core.global.exception;

import java.util.List;

public class ValidationException extends BaseException {
    private final List<String> validationErrors;

    public ValidationException(String message, List<String> validationErrors) {
        super(message, "VALIDATION_ERROR", "VALIDATION", 400);
        this.validationErrors = validationErrors;
    }

    public ValidationException(String message, String errorCode, List<String> validationErrors) {
        super(message, errorCode, "VALIDATION", 400);
        this.validationErrors = validationErrors;
    }

    public ValidationException(String message, String errorCode, String errorType, int httpStatus,
            List<String> validationErrors) {
        super(message, errorCode, errorType, httpStatus);
        this.validationErrors = validationErrors;
    }

    public ValidationException(String message, String errorCode, String errorType, int httpStatus, Throwable cause,
            List<String> validationErrors) {
        super(message, errorCode, errorType, httpStatus, cause);
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}