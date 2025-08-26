package com.sleekydz86.finsight.core.global.exception;

public class AuthenticationFailedException extends BaseException {
    private final String email;

    public AuthenticationFailedException(String email) {
        super("로그인에 실패했습니다. 이메일 또는 비밀번호를 확인해주세요",
                "AUTH_001", "Authentication Failed", 401);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}