package com.sleekydz86.finsight.core.inbox.domain.port.in.dto;

import com.sleekydz86.finsight.core.inbox.domain.InboxCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "관리자 인앱 알림 일괄 등록")
public record InboxBroadcastRequest(
        @NotNull @Schema(description = "카테고리", requiredMode = Schema.RequiredMode.REQUIRED)
        InboxCategory category,
        @NotBlank @Size(max = 500)
        @Schema(description = "알림 문구", requiredMode = Schema.RequiredMode.REQUIRED, example = "새로운 공지가 등록되었습니다.")
        String title,
        @Size(max = 1000)
        @Schema(description = "부가 스니펫")
        String body,
        @Size(max = 500)
        @Schema(description = "이동 URL", example = "/admin/moderation")
        String linkUrl,
        @Size(max = 100)
        @Schema(description = "행위자 표시명", example = "FinSight")
        String actorName,
        @Size(max = 500)
        @Schema(description = "행위자 아바타 URL")
        String actorAvatarUrl,
        @Schema(description = "전체 사용자 대상 여부 (true면 userIds 무시)", example = "true")
        Boolean allUsers,
        @Schema(description = "ADMIN/MANAGER만 대상", example = "false")
        Boolean adminsOnly,
        @Schema(description = "특정 사용자 ID 목록 (allUsers=false, adminsOnly=false일 때)")
        List<Long> userIds
) {
}
