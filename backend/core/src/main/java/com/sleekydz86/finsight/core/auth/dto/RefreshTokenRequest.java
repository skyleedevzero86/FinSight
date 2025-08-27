package com.sleekydz86.finsight.core.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshTokenRequest {
    @NotBlank(message = "리프레시 토큰은 필수입니다")
    private String refreshToken;

    public RefreshTokenRequest() {}

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}