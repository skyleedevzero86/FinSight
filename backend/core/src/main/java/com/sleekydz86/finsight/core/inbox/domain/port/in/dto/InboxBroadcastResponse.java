package com.sleekydz86.finsight.core.inbox.domain.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 알림 일괄 등록 결과")
public record InboxBroadcastResponse(
        @Schema(description = "생성된 알림 건수") int createdCount
) {
}
