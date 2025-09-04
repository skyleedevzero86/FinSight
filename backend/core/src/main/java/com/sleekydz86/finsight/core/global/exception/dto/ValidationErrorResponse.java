package com.sleekydz86.finsight.core.global.exception.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ValidationErrorResponse extends ErrorResponse {
    private final List<String> errors;

    public ValidationErrorResponse() {
        super();
        this.errors = List.of();
    }

    public ValidationErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path, List<String> errors) {
        super(timestamp, status, error, message, path);
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}