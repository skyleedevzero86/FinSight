package com.sleekydz86.finsight.core.notification.domain;

public enum EmailActorType {
    ANONYMOUS("비로그인"),
    USER("로그인 사용자"),
    SYSTEM("시스템"),
    ADMIN("관리자");

    private final String label;

    EmailActorType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
