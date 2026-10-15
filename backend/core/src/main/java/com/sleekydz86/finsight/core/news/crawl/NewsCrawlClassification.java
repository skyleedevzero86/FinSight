package com.sleekydz86.finsight.core.news.crawl;

public record NewsCrawlClassification(
        NewsCrawlCause cause,
        Integer httpStatus,
        Integer retryAfterSeconds) {
}
