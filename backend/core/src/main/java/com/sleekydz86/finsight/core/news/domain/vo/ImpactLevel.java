package com.sleekydz86.finsight.core.news.domain.vo;

public enum ImpactLevel {
    LOW("낮음"),
    MEDIUM("보통"),
    HIGH("높음"),
    CRITICAL("매우 높음");

    private final String description;

    ImpactLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}