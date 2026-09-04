package com.sleekydz86.finsight.core.inbox.domain.port.in.dto;

import com.sleekydz86.finsight.core.inbox.domain.InboxCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "인앱 알림 항목")
public record InboxItemResponse(
        @Schema(description = "알림 ID") Long id,
        @Schema(description = "카테고리") InboxCategory category,
        @Schema(description = "행위자 사용자 ID") Long actorUserId,
        @Schema(description = "행위자 표시명") String actorName,
        @Schema(description = "행위자 아바타") String actorAvatarUrl,
        @Schema(description = "알림 문구") String title,
        @Schema(description = "부가 스니펫") String body,
        @Schema(description = "이동 URL") String linkUrl,
        @Schema(description = "연관 타입") String refType,
        @Schema(description = "연관 ID") Long refId,
        @Schema(description = "읽음 여부") boolean read,
        @Schema(description = "생성 시각") LocalDateTime createdAt
) {
}
