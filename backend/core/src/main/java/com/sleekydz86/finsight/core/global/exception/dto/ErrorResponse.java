package com.sleekydz86.finsight.core.global.exception.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String requestId;
    private String errorCode;
    private String message;
    private String path;
    private int status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    private Map<String, Object> context;
    private String traceId;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }

    public static class ErrorResponseBuilder {
        private ErrorResponse errorResponse = new ErrorResponse();

        public ErrorResponseBuilder requestId(String requestId) {
            errorResponse.requestId = requestId;
            return this;
        }

        public ErrorResponseBuilder errorCode(String errorCode) {
            errorResponse.errorCode = errorCode;
            return this;
        }

        public ErrorResponseBuilder message(String message) {
            errorResponse.message = message;
            return this;
        }

        public ErrorResponseBuilder path(String path) {
            errorResponse.path = path;
            return this;
        }

        public ErrorResponseBuilder status(int status) {
            errorResponse.status = status;
            return this;
        }

        public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
            errorResponse.timestamp = timestamp;
            return this;
        }

        public ErrorResponseBuilder context(Map<String, Object> context) {
            errorResponse.context = context;
            return this;
        }

        public ErrorResponseBuilder traceId(String traceId) {
            errorResponse.traceId = traceId;
            return this;
        }

        public ErrorResponse build() {
            return errorResponse;
        }
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}