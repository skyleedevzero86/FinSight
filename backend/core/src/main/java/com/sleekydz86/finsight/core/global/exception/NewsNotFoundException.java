package com.sleekydz86.finsight.core.global.exception;

public class NewsNotFoundException extends BaseException {
    private final Long newsId;

    public NewsNotFoundException(Long newsId) {
        super("뉴스를 찾을 수 없습니다. ID: " + newsId,
                "NEWS_001", "News Not Found", 404);
        this.newsId = newsId;
    }

    public Long getNewsId() {
        return newsId;
    }
}