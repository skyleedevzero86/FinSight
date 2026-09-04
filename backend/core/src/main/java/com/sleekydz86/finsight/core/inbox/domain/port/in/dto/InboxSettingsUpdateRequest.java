package com.sleekydz86.finsight.core.inbox.domain.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "인앱 알림 수신 설정 변경")
public record InboxSettingsUpdateRequest(
        @NotNull @Schema(description = "유튜브(미디어) 알림", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean youtubeEnabled,
        @NotNull @Schema(description = "뉴스 알림", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean newsEnabled,
        @NotNull @Schema(description = "댓글 알림", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean commentEnabled,
        @NotNull @Schema(description = "QnA 알림", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean qnaEnabled
) {
}
