package com.sleekydz86.finsight.core.portfolio.service;

public final class PortfolioShareSignals {

    public static final int NORMAL_LIKES = 10;

    private PortfolioShareSignals() {
    }

    public static String display(String stored, long dislikes, long reports) {
        if ("REMOVED".equals(stored)) {
            return "REMOVED";
        }
        if ("BLIND".equals(stored) || reports > 0) {
            return "BLIND";
        }
        if ("WARN".equals(stored) || dislikes > 0) {
            return "WARN";
        }
        return "OPEN";
    }

    public static boolean normal(String display, long likes) {
        return "OPEN".equals(display) && likes >= NORMAL_LIKES;
    }
}
