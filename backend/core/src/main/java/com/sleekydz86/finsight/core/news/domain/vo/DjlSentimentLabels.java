package com.sleekydz86.finsight.core.news.domain.vo;

import java.util.Locale;

public final class DjlSentimentLabels {

    private DjlSentimentLabels() {
    }

    public static SentimentType toType(String raw) {
        if (raw == null || raw.isBlank()) {
            return SentimentType.NEUTRAL;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.contains("positive") || value.equals("pos")) {
            return SentimentType.POSITIVE;
        }
        if (value.contains("negative") || value.equals("neg")) {
            return SentimentType.NEGATIVE;
        }
        if (value.contains("neutral") || value.equals("neu")) {
            return SentimentType.NEUTRAL;
        }
        String number = value.startsWith("label_") ? value.substring("label_".length()) : value;
        return switch (number) {
            case "0" -> SentimentType.NEGATIVE;
            case "2" -> SentimentType.POSITIVE;
            default -> SentimentType.NEUTRAL;
        };
    }
}
