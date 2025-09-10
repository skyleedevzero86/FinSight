package com.sleekydz86.finsight.core.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public class OtpLoginRequest {
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    @NotBlank(message = "OTP 코드는 필수입니다")
    @Pattern(regexp = "\\d{6}", message = "OTP 코드는 6자리 숫자여야 합니다")
    private String otpCode;

    public OtpLoginRequest() {}

    public OtpLoginRequest(String email, String password, String otpCode) {
        this.email = email;
        this.password = password;
        this.otpCode = otpCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
}