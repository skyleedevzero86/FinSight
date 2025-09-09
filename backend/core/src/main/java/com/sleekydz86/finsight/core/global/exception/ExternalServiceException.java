package com.sleekydz86.finsight.core.global.exception;

public class ExternalServiceException extends BaseException {
    private final String serviceName;
    private final int statusCode;

    public ExternalServiceException(String message, String serviceName, int statusCode) {
        super(message, "EXTERNAL_SERVICE_ERROR", "EXTERNAL_SERVICE", statusCode);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
    }

    public ExternalServiceException(String message, String serviceName, int statusCode, Throwable cause) {
        super(message, "EXTERNAL_SERVICE_ERROR", "EXTERNAL_SERVICE", statusCode, cause);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getStatusCode() {
        return statusCode;
    }
}