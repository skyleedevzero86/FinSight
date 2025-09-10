package com.sleekydz86.finsight.core.auth.dto;

public class OtpVerifyResponse {
    private final boolean success;
    private final String message;

    public OtpVerifyResponse() {
        this.success = false;
        this.message = null;
    }

    public OtpVerifyResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}