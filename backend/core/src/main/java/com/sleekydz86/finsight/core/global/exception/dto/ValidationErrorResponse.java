package com.sleekydz86.finsight.core.global.exception.dto;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@SuperBuilder
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
}