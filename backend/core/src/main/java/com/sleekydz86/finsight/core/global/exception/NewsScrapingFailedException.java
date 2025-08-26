package com.sleekydz86.finsight.core.global.exception;

public class NewsScrapingFailedException extends BaseException {
    private final String provider;
    private final String reason;

    public NewsScrapingFailedException(String provider, String reason) {
        super("뉴스 수집에 실패했습니다. 제공자: " + provider + ", 사유: " + reason,
                "NEWS_002", "News Scraping Failed", 500);
        this.provider = provider;
        this.reason = reason;
    }

    public String getProvider() {
        return provider;
    }

    public String getReason() {
        return reason;
    }
}