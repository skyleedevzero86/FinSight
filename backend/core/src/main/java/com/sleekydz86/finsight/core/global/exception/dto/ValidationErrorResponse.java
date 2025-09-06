package com.sleekydz86.finsight.core.global.exception.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationErrorResponse {
    private String requestId;
    private String message;
    private List<String> validationErrors;
    private Map<String, String> fieldErrors;
    private String path;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public ValidationErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public static ValidationErrorResponseBuilder builder() {
        return new ValidationErrorResponseBuilder();
    }

    public static class ValidationErrorResponseBuilder {
        private ValidationErrorResponse validationErrorResponse = new ValidationErrorResponse();

        public ValidationErrorResponseBuilder requestId(String requestId) {
            validationErrorResponse.requestId = requestId;
            return this;
        }

        public ValidationErrorResponseBuilder message(String message) {
            validationErrorResponse.message = message;
            return this;
        }

        public ValidationErrorResponseBuilder validationErrors(List<String> validationErrors) {
            validationErrorResponse.validationErrors = validationErrors;
            return this;
        }

        public ValidationErrorResponseBuilder fieldErrors(Map<String, String> fieldErrors) {
            validationErrorResponse.fieldErrors = fieldErrors;
            return this;
        }

        public ValidationErrorResponseBuilder path(String path) {
            validationErrorResponse.path = path;
            return this;
        }

        public ValidationErrorResponseBuilder timestamp(LocalDateTime timestamp) {
            validationErrorResponse.timestamp = timestamp;
            return this;
        }

        public ValidationErrorResponse build() {
            return validationErrorResponse;
        }
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}