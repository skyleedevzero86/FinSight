package com.sleekydz86.finsight.core.global.exception;

public class SystemException extends BaseException {

    public SystemException(String message) {
        super(message, "SYSTEM_ERROR", "SYSTEM", 500);
    }

    public SystemException(String message, Throwable cause) {
        super(message, "SYSTEM_ERROR", "SYSTEM", 500, cause);
    }

    public SystemException(String message, String errorCode) {
        super(message, errorCode, "SYSTEM", 500);
    }

    public SystemException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, "SYSTEM", 500, cause);
    }

    public SystemException(String message, String errorCode, String errorType, int httpStatus) {
        super(message, errorCode, errorType, httpStatus);
    }

    public SystemException(String message, String errorCode, String errorType, int httpStatus, Throwable cause) {
        super(message, errorCode, errorType, httpStatus, cause);
    }
}