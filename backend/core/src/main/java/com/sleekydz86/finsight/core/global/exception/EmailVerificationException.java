package com.sleekydz86.finsight.core.global.exception;

public class EmailVerificationException extends BaseException {

    public EmailVerificationException(String message) {
        super(message, "EMAIL_VERIFY_001", "Email Verification", 400);
    }
}
