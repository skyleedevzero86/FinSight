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
            Boolean favorited,
            long likeCount,
            long dislikeCount,
            String myReaction
    ) {
    }

    public record FavoriteToggleResponse(
            boolean favorited,
            long favoriteCount
    ) {
    }

    public record ReactionToggleRequest(
            String reaction
    ) {
    }

    public record ReactionToggleResponse(
            String myReaction,
            long likeCount,
            long dislikeCount
    ) {
    }

    public record CommentCreateRequest(
            String content,
            Long parentId
    ) {
    }

    public record CommentPageResponse(
            List<CommentResponse> items,
            int page,
            int size,
            long totalElements,
            long totalComments,
            boolean hasNext
    ) {
        public CommentPageResponse {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record ReplyPageResponse(
            Long parentId,
            List<CommentResponse> items,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrev
    ) {
        public ReplyPageResponse {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record CommentResponse(
            Long id,
            String videoId,
            String userEmail,
            String authorNickname,
            String content,
            Long parentId,
            LocalDateTime createdAt,
            List<CommentResponse> replies,
            long replyCount,
            int replyPage,
            int replyTotalPages
    ) {
        public CommentResponse {
            replies = replies == null ? List.of() : List.copyOf(replies);
        }
    }
}
