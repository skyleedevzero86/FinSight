package com.sleekydz86.finsight.core.global.exception;

public class OtpNotEnabledException extends OtpException {

    public OtpNotEnabledException(String email) {
        super("OTP가 활성화되지 않았습니다: " + email);
    }
}