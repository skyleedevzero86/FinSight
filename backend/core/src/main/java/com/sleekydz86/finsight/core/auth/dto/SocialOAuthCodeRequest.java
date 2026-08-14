package com.sleekydz86.finsight.core.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class SocialOAuthCodeRequest {
    @NotBlank(message = "인가 코드는 필수입니다")
    private String code;

    private String state;

    public SocialOAuthCodeRequest() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
