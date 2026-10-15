package com.sleekydz86.finsight.core.news.crawl;

import com.sleekydz86.finsight.core.global.NewsProvider;

import java.time.LocalDateTime;

public record NewsCrawlIncident(
        Long id,
        NewsProvider provider,
        NewsCrawlIncidentStatus status,
        NewsCrawlCause cause,
        Integer httpStatus,
        Integer retryAfterSeconds,
        String message,
        boolean retryable,
        LocalDateTime occurredAt,
        LocalDateTime analyzedAt,
        String modelNote,
        String approvedBy,
        LocalDateTime approvedAt,
        long fencingToken) {

    public NewsCrawlIncident copy(
            NewsCrawlIncidentStatus status,
            boolean retryable,
            LocalDateTime analyzedAt,
            String modelNote,
            String approvedBy,
            LocalDateTime approvedAt,
            long fencingToken) {
        return new NewsCrawlIncident(
                id,
                provider,
                status,
                cause,
                httpStatus,
                retryAfterSeconds,
                message,
                retryable,
                occurredAt,
                analyzedAt,
                modelNote,
                approvedBy,
                approvedAt,
                fencingToken);
    }
}
