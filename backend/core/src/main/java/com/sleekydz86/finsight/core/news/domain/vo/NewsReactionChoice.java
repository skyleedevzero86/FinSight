package com.sleekydz86.finsight.core.news.domain.vo;

import com.sleekydz86.finsight.core.global.exception.ValidationException;

import java.util.List;

public final class NewsReactionChoice {

    private NewsReactionChoice() {
    }

    public static String normalize(String requested) {
        if (requested == null || requested.isBlank()) {
            throw invalid(requested);
        }
        String value = requested.trim().toUpperCase();
        if (!"LIKE".equals(value) && !"DISLIKE".equals(value)) {
            throw invalid(requested);
        }
        return value;
    }

    public static String apply(String current, String requested) {
        String next = normalize(requested);
        if (current == null || current.isBlank() || !current.equals(next)) {
            return next;
        }
        return null;
    }

    private static ValidationException invalid(String requested) {
        return new ValidationException(
                "반응은 LIKE 또는 DISLIKE 여야 합니다. 입력값: " + requested,
                List.of("reaction"));
    }
}
