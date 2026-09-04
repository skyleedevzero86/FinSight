package com.sleekydz86.finsight.core.inbox.domain;

public enum InboxCategory {
    YOUTUBE("유튜브"),
    NEWS("뉴스"),
    COMMENT("댓글"),
    QNA("QnA"),
    WATCHLIST("관심종목"),
    ADMIN("관리");

    private final String label;

    InboxCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
