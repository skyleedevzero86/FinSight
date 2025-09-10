package com.sleekydz86.finsight.core.auth.dto;

public class OtpSetupResponse {
    private final String secret;
    private final String qrCode;
    private final String message;

    public OtpSetupResponse() {
        this.secret = null;
        this.qrCode = null;
        this.message = null;
    }

    public OtpSetupResponse(String secret, String qrCode, String message) {
        this.secret = secret;
        this.qrCode = qrCode;
        this.message = message;
    }

    public String getSecret() {
        return secret;
    }

    public String getQrCode() {
        return qrCode;
    }

    public String getMessage() {
        return message;
    }
}