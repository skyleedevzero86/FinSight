package com.sleekydz86.finsight.core.news.crawl;

import java.time.LocalDateTime;

public final class NewsCrawlRetryPolicy {

    private NewsCrawlRetryPolicy() {
    }

    public static boolean retryable(
            NewsCrawlCause cause,
            Integer retryAfterSeconds,
            LocalDateTime occurredAt,
            LocalDateTime now) {
        return switch (cause) {
            case CONNECTION, TIMEOUT, HTTP_5XX -> true;
            case HTTP_429 -> windowElapsed(retryAfterSeconds, occurredAt, now);
            default -> false;
        };
    }

    private static boolean windowElapsed(Integer retryAfterSeconds, LocalDateTime occurredAt, LocalDateTime now) {
        if (retryAfterSeconds == null || retryAfterSeconds < 0 || occurredAt == null || now == null) {
            return false;
        }
        return !now.isBefore(occurredAt.plusSeconds(retryAfterSeconds));
    }
}
