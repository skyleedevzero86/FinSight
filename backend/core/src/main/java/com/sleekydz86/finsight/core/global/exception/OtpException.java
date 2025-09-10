package com.sleekydz86.finsight.core.global.exception;

public class OtpException extends BaseException {

    private static final String DEFAULT_ERROR_CODE = "OTP_ERROR";
    private static final String DEFAULT_ERROR_TYPE = "OTP_AUTHENTICATION";
    private static final int DEFAULT_HTTP_STATUS = 400;

    public OtpException(String message) {
        super(message, DEFAULT_ERROR_CODE, DEFAULT_ERROR_TYPE, DEFAULT_HTTP_STATUS);
    }

    public OtpException(String message, Throwable cause) {
        super(message, DEFAULT_ERROR_CODE, DEFAULT_ERROR_TYPE, DEFAULT_HTTP_STATUS, cause);
    }

    public OtpException(String message, String errorCode) {
        super(message, errorCode, DEFAULT_ERROR_TYPE, DEFAULT_HTTP_STATUS);
    }

    public OtpException(String message, String errorCode, int httpStatus) {
        super(message, errorCode, DEFAULT_ERROR_TYPE, httpStatus);
    }

    public OtpException(String message, String errorCode, String errorType, int httpStatus) {
        super(message, errorCode, errorType, httpStatus);
    }

    public OtpException(String message, String errorCode, String errorType, int httpStatus, Throwable cause) {
        super(message, errorCode, errorType, httpStatus, cause);
    }
}