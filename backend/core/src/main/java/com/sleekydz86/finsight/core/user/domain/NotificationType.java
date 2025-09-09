package com.sleekydz86.finsight.core.user.domain;

public enum NotificationType {
    EMAIL("이메일 알림"),
    SMS("SMS 알림"),
    PUSH("푸시 알림"),
    IN_APP("앱 내 알림"),
    NEWS_ALERT("뉴스 알림"),
    PRICE_ALERT("가격 알림"),
    MARKET_SUMMARY("시장 요약"),
    PORTFOLIO_UPDATE("포트폴리오 업데이트");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}