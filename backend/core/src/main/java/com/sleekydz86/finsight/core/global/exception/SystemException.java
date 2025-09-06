package com.sleekydz86.finsight.core.global.exception;

public class SystemException extends BaseException {
    public SystemException(String message) {
        super(message);
    }

    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }

    public SystemException(String message, String errorCode) {
        super(message, errorCode);
    }

    public SystemException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}