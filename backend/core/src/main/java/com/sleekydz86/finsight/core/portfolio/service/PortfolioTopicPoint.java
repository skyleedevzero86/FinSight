package com.sleekydz86.finsight.core.portfolio.service;

public record PortfolioTopicPoint(long shareId, String personKey, String label, float[] vector) {

    public static String personKey(long userId, String authorName) {
        if (userId > 0) {
            return "user:" + userId;
        }
        String name = authorName == null ? "" : authorName.trim();
        return "name:" + name;
    }
}
