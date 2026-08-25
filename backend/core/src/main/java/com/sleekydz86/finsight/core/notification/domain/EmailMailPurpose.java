package com.sleekydz86.finsight.core.notification.domain;

public enum EmailMailPurpose {
    VERIFICATION_SIGNUP("회원가입 인증"),
    VERIFICATION_FIND_EMAIL("이메일 찾기"),
    VERIFICATION_FIND_PASSWORD("비밀번호 찾기"),
    NEWS_ALERT("뉴스 알림"),
    SYSTEM_NOTIFICATION("시스템 알림"),
    WELCOME("환영 메일"),
    PASSWORD_CHANGE_REMINDER("비밀번호 변경 안내"),
    ACCOUNT_RECOVERY_OTP("계정 복구 OTP"),
    PASSWORD_RESET_CONFIRMATION("비밀번호 재설정 확인"),
    OTHER("기타");

    private final String label;

    EmailMailPurpose(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
