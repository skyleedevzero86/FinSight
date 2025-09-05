package com.sleekydz86.finsight.core.global.exception;

import java.time.LocalDateTime;

public abstract class BaseException extends RuntimeException {
    private final String errorCode;
    private final String errorType;
    private final int httpStatus;
    private final LocalDateTime timestamp;

    public BaseException(String message, String errorCode, String errorType, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.errorType = errorType;
        this.httpStatus = httpStatus;
        this.timestamp = LocalDateTime.now();
    }

    public BaseException(String message, String errorCode, String errorType, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorType = errorType;
        this.httpStatus = httpStatus;
        this.timestamp = LocalDateTime.now();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorType() {
        return errorType;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}