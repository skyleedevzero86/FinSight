package com.sleekydz86.finsight.core.global.exception;

public class YoutubeSourceNotFoundException extends BaseException {
    private final Long sourceId;

    public YoutubeSourceNotFoundException(Long sourceId) {
        super("수집 소스를 찾을 수 없습니다. ID: " + sourceId, "YOUTUBE_SOURCE_001", "YOUTUBE", 404);
        this.sourceId = sourceId;
    }

    public Long getSourceId() {
        return sourceId;
    }
}
