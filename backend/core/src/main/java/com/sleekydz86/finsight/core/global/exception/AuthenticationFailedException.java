package com.sleekydz86.finsight.core.global.exception;

public class AuthenticationFailedException extends BaseException {
    private final String email;

    public AuthenticationFailedException(String email) {
        super("로그인에 실패했습니다. 이메일(또는 아이디)과 비밀번호를 확인해 주세요",
                "AUTH_001", "인증 실패", 401);
        this.email = email;
    }

    public AuthenticationFailedException(String email, String message) {
        super(message, "AUTH_001", "인증 실패", 401);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}