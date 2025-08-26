package com.sleekydz86.finsight.core.global.exception;

import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {
    private final String errorCode;
    private final String errorType;
    private final int httpStatus;

    protected BaseException(String message, String errorCode, String errorType, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.errorType = errorType;
        this.httpStatus = httpStatus;
    }

    protected BaseException(String message, String errorCode, String errorType, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorType = errorType;
        this.httpStatus = httpStatus;
    }
}