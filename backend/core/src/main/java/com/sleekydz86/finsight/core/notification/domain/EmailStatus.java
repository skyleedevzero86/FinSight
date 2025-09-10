package com.sleekydz86.finsight.core.notification.domain;

public enum EmailStatus {
    PENDING("전송 대기"),
    SENT("전송 완료"),
    DELIVERED("전달 완료"),
    OPENED("열람"),
    CLICKED("클릭"),
    BOUNCED("반송"),
    FAILED("전송 실패"),
    SPAM("스팸 신고");

    private final String description;

    EmailStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}