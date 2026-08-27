package com.sleekydz86.finsight.core.media.livevod.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class LiveVodEngagementDtos {
    private LiveVodEngagementDtos() {
    }

    public record EngagementSummary(
            String videoId,
            long favoriteCount,
            long commentCount,
            Boolean favorited
    ) {
    }

    public record FavoriteToggleResponse(
            boolean favorited,
            long favoriteCount
    ) {
    }

    public record CommentCreateRequest(
            String content,
            Long parentId
    ) {
    }

    public record CommentResponse(
            Long id,
            String videoId,
            String userEmail,
            String authorNickname,
            String content,
            Long parentId,
            LocalDateTime createdAt,
            List<CommentResponse> replies
    ) {
        public CommentResponse {
            replies = replies == null ? List.of() : List.copyOf(replies);
        }
    }
}
