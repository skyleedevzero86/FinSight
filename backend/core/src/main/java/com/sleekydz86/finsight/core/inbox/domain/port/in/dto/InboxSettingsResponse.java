package com.sleekydz86.finsight.core.inbox.domain.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인앱 알림 수신 설정")
public record InboxSettingsResponse(
        @Schema(description = "유튜브(미디어) 알림") boolean youtubeEnabled,
        @Schema(description = "뉴스 알림") boolean newsEnabled,
        @Schema(description = "댓글 알림") boolean commentEnabled,
        @Schema(description = "QnA 알림") boolean qnaEnabled
) {
}
