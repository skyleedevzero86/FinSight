package com.sleekydz86.finsight.core.global.exception;

public class OtpVerificationFailedException extends OtpException {

    public OtpVerificationFailedException(String email) {
        super("OTP 검증에 실패했습니다: " + email);
    }
}