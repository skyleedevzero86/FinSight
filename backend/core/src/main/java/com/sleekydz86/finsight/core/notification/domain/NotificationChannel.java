package com.sleekydz86.finsight.core.notification.domain;

public enum NotificationChannel {
    EMAIL("이메일"),
    SMS("SMS"),
    PUSH("푸시 알림"),
    IN_APP("앱 내 알림"),
    KAKAO("카카오톡"),
    SLACK("슬랙"),
    WEBHOOK("웹훅");

    private final String description;

    NotificationChannel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}