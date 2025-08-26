package com.sleekydz86.finsight.core.global.exception.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ValidationErrorResponse {
    private final String errorCode;
    private final String errorType;
    private final String message;
    private final String userMessage;
    private final int httpStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime timestamp;

    private final String path;
    private final String method;

    private final List<FieldError> fieldErrors;
    private final Map<String, Object> additionalInfo;

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String message;
        private final String userMessage;
        private final Object rejectedValue;
        private final String errorCode;
    }
}