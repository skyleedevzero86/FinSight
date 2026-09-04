package com.sleekydz86.finsight.core.notification.domain;

public enum SmsPurpose {
    NEWS_ALERT("뉴스 알림"),
    OTP("OTP"),
    ACCOUNT_RECOVERY("계정 복구"),
    SYSTEM("시스템 알림"),
    NOTIFICATION("일반 알림"),
    MANUAL("관리자 수동");

    private final String label;

    SmsPurpose(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
