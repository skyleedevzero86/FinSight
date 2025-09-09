package com.sleekydz86.finsight.core.user.domain;

public enum UserStatus {
    PENDING("승인 대기"),
    APPROVED("승인 완료"),
    REJECTED("승인 거부"),
    SUSPENDED("계정 정지"),
    WITHDRAWN("탈퇴");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this == APPROVED;
    }

    public boolean canLogin() {
        return this == APPROVED;
    }
}