package com.sleekydz86.finsight.core.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

public class OtpSetupRequest {
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    public OtpSetupRequest() {}

    public OtpSetupRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}