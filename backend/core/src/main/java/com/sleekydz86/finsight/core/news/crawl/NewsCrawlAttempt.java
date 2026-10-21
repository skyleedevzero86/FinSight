package com.sleekydz86.finsight.core.news.crawl;

import java.time.LocalDateTime;

public record NewsCrawlAttempt(
        Long id,
        long incidentId,
        NewsCrawlAttemptStatus status,
        String executor,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        int savedCount,
        int duplicateCount,
        String resultMessage) {
}
