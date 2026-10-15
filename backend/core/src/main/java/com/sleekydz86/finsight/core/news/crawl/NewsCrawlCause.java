package com.sleekydz86.finsight.core.news.crawl;

public enum NewsCrawlCause {
    CONNECTION,
    TIMEOUT,
    HTTP_401,
    HTTP_403,
    HTTP_404,
    HTTP_429,
    HTTP_5XX,
    PARSE,
    UNEXPECTED
}
