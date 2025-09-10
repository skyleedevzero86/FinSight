package com.sleekydz86.finsight.core.notification.domain;

public enum NotificationStatus {
    PENDING("대기중"),
    SENT("발송완료"),
    FAILED("발송실패"),
    CANCELLED("취소됨");

    private final String description;

    NotificationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}