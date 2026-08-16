package com.sleekydz86.finsight.core.auth.email;

public enum EmailVerificationPurpose {
    SIGNUP("회원가입인증"),
    FIND_EMAIL("아이디찾기인증"),
    FIND_PASSWORD("비밀번호찾기인증");

    private final String label;

    EmailVerificationPurpose(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
