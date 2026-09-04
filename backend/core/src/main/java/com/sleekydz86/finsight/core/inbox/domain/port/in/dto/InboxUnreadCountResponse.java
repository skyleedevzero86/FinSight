package com.sleekydz86.finsight.core.inbox.domain.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인앱 알림 미읽음 수")
public record InboxUnreadCountResponse(
        @Schema(description = "미읽음 건수") long unreadCount
) {
}
