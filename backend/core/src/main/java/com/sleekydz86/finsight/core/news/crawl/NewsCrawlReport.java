package com.sleekydz86.finsight.core.news.crawl;

import java.time.LocalDateTime;

public record NewsCrawlReport(
        long incidentId,
        String provider,
        NewsCrawlIncidentStatus incidentStatus,
        NewsCrawlCause cause,
        boolean retryable,
        boolean approvalRequired,
        String approver,
        LocalDateTime approvedAt,
        String executor,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String result,
        int savedCount,
        int duplicateCount,
        String modelNote) {
}
