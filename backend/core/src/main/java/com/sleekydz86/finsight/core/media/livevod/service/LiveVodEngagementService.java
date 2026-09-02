package com.sleekydz86.finsight.core.media.livevod.service;

import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodCommentJpaEntity;
import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodCommentJpaRepository;
import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodFavoriteJpaEntity;
import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodFavoriteJpaRepository;
import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodReactionJpaEntity;
import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodReactionJpaRepository;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.CommentCreateRequest;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.CommentPageResponse;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.CommentResponse;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.EngagementSummary;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.FavoriteToggleResponse;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.ReactionToggleResponse;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.ReplyPageResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.LiveVodFeedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LiveVodEngagementService {

    public static final String REACTION_LIKE = "LIKE";
    public static final String REACTION_DISLIKE = "DISLIKE";

    private final LiveVodFavoriteJpaRepository favoriteRepository;
    private final LiveVodCommentJpaRepository commentRepository;
    private final LiveVodReactionJpaRepository reactionRepository;

    public LiveVodEngagementService(
            LiveVodFavoriteJpaRepository favoriteRepository,
            LiveVodCommentJpaRepository commentRepository,
            LiveVodReactionJpaRepository reactionRepository) {
        this.favoriteRepository = favoriteRepository;
        this.commentRepository = commentRepository;
        this.reactionRepository = reactionRepository;
    }

    @Transactional(readOnly = true)
    public LiveVodFeedResponse enrichFeed(LiveVodFeedResponse feed) {
        if (feed == null || feed.sections() == null || feed.sections().isEmpty()) {
            return feed;
        }
        Set<String> ids = feed.sections().stream()
                .flatMap(section -> section.items().stream())
                .map(LiveVodFeedResponse.LiveVodItemResponse::videoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (feed.featuredVideoId() != null && !feed.featuredVideoId().isBlank()) {
            ids.add(feed.featuredVideoId());
        }
        Map<String, EngagementSummary> map = summarizeMany(ids, null);

        List<LiveVodFeedResponse.LiveVodSectionResponse> sections = feed.sections().stream()
                .map(section -> new LiveVodFeedResponse.LiveVodSectionResponse(
                        section.heading(),
                        section.items().stream().map(item -> withCounts(item, map)).toList()))
                .toList();

        return new LiveVodFeedResponse(
                feed.title(),
                feed.tab(),
                feed.featuredVideoId(),
                feed.featuredTitle(),
                feed.featuredThumbnailUrl(),
                sections);
    }

    @Transactional(readOnly = true)
    public EngagementSummary getEngagement(String videoId, String userEmail) {
        return summarizeOne(normalizeVideoId(videoId), userEmail);
    }

    @Transactional
    public FavoriteToggleResponse toggleFavorite(String videoId, String userEmail) {
        String id = normalizeVideoId(videoId);
        requireUser(userEmail);
        Optional<LiveVodFavoriteJpaEntity> existing = favoriteRepository.findByUserEmailAndVideoId(userEmail, id);
        boolean favorited;
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            favorited = false;
        } else {
            favoriteRepository.save(new LiveVodFavoriteJpaEntity(id, userEmail));
            favorited = true;
        }
        return new FavoriteToggleResponse(favorited, favoriteRepository.countByVideoId(id));
    }

    @Transactional
    public ReactionToggleResponse toggleReaction(String videoId, String userEmail, String reactionRaw) {
        String id = normalizeVideoId(videoId);
        requireUser(userEmail);
        String reaction = normalizeReaction(reactionRaw);
        Optional<LiveVodReactionJpaEntity> existing = reactionRepository.findByUserEmailAndVideoId(userEmail, id);
        String myReaction = null;
        if (existing.isPresent()) {
            LiveVodReactionJpaEntity row = existing.get();
            if (reaction.equals(row.getReactionType())) {
                reactionRepository.delete(row);
            } else {
                row.setReactionType(reaction);
                reactionRepository.save(row);
                myReaction = reaction;
            }
        } else {
            reactionRepository.save(new LiveVodReactionJpaEntity(id, userEmail, reaction));
            myReaction = reaction;
        }
        long[] counts = reactionCounts(id);
        return new ReactionToggleResponse(myReaction, counts[0], counts[1]);
    }

    public static final int ROOT_COMMENT_PAGE_SIZE = 15;
    public static final int REPLY_PAGE_SIZE = 5;

    @Transactional(readOnly = true)
    public CommentPageResponse listComments(String videoId, int page, int size) {
        String id = normalizeVideoId(videoId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? ROOT_COMMENT_PAGE_SIZE : Math.min(size, 50);
        Page<LiveVodCommentJpaEntity> rootPage = commentRepository
                .findByVideoIdAndParentIsNullOrderByCreatedAtDesc(id, PageRequest.of(safePage, safeSize));

        List<CommentResponse> items = rootPage.getContent().stream()
                .map(row -> toRootComment(row, 0, REPLY_PAGE_SIZE))
                .toList();

        return new CommentPageResponse(
                items,
                rootPage.getNumber(),
                rootPage.getSize(),
                rootPage.getTotalElements(),
                commentRepository.countByVideoId(id),
                rootPage.hasNext());
    }

    @Transactional(readOnly = true)
    public ReplyPageResponse listReplies(String videoId, Long parentId, int page, int size) {
        String id = normalizeVideoId(videoId);
        if (parentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "parentId가 필요합니다.");
        }
        LiveVodCommentJpaEntity parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "부모 댓글을 찾을 수 없습니다."));
        if (!id.equals(parent.getVideoId()) || parent.getParentId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 부모 댓글입니다.");
        }

        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? REPLY_PAGE_SIZE : Math.min(size, 20);
        Page<LiveVodCommentJpaEntity> replyPage = commentRepository
                .findByParent_IdOrderByCreatedAtAsc(parentId, PageRequest.of(safePage, safeSize));

        List<CommentResponse> items = replyPage.getContent().stream()
                .map(this::toLeafComment)
                .toList();

        return new ReplyPageResponse(
                parentId,
                items,
                replyPage.getNumber(),
                replyPage.getSize(),
                replyPage.getTotalElements(),
                replyPage.getTotalPages(),
                replyPage.hasNext(),
                replyPage.hasPrevious());
    }

    @Transactional
    public CommentResponse createComment(
            String videoId,
            String userEmail,
            String nickname,
            CommentCreateRequest request) {
        String id = normalizeVideoId(videoId);
        requireUser(userEmail);
        if (request == null || request.content() == null || request.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "댓글 내용을 입력해 주세요.");
        }
        String content = request.content().trim();
        if (content.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "댓글은 2000자 이하여야 합니다.");
        }

        LiveVodCommentJpaEntity parent = null;
        if (request.parentId() != null) {
            parent = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "부모 댓글을 찾을 수 없습니다."));
            if (!id.equals(parent.getVideoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "다른 영상의 댓글에는 답글할 수 없습니다.");
            }
            if (parent.getParentId() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대댓글에는 다시 답글을 달 수 없습니다.");
            }
        }

        LiveVodCommentJpaEntity saved = commentRepository.save(
                new LiveVodCommentJpaEntity(id, userEmail, nickname, content, parent));
        if (parent == null) {
            return toRootComment(saved, 0, REPLY_PAGE_SIZE);
        }
        return toLeafComment(saved);
    }

    private CommentResponse toRootComment(LiveVodCommentJpaEntity row, int replyPage, int replySize) {
        Page<LiveVodCommentJpaEntity> replies = commentRepository
                .findByParent_IdOrderByCreatedAtAsc(row.getId(), PageRequest.of(Math.max(replyPage, 0), replySize));
        List<CommentResponse> replyItems = replies.getContent().stream()
                .map(this::toLeafComment)
                .toList();
        return new CommentResponse(
                row.getId(),
                row.getVideoId(),
                maskEmail(row.getUserEmail()),
                row.getAuthorNickname(),
                row.getContent(),
                row.getParentId(),
                row.getCreatedAt(),
                replyItems,
                replies.getTotalElements(),
                replies.getNumber(),
                Math.max(replies.getTotalPages(), 0));
    }

    private CommentResponse toLeafComment(LiveVodCommentJpaEntity row) {
        return new CommentResponse(
                row.getId(),
                row.getVideoId(),
                maskEmail(row.getUserEmail()),
                row.getAuthorNickname(),
                row.getContent(),
                row.getParentId(),
                row.getCreatedAt(),
                List.of(),
                0,
                0,
                0);
    }

    private LiveVodFeedResponse.LiveVodItemResponse withCounts(
            LiveVodFeedResponse.LiveVodItemResponse item,
            Map<String, EngagementSummary> map) {
        EngagementSummary summary = map.getOrDefault(
                item.videoId(),
                emptySummary(item.videoId()));
        return new LiveVodFeedResponse.LiveVodItemResponse(
                item.videoId(),
                item.title(),
                item.thumbnailUrl(),
                item.watchUrl(),
                item.embedUrl(),
                item.channelTitle(),
                summary.favoriteCount(),
                summary.commentCount());
    }

    private Map<String, EngagementSummary> summarizeMany(Set<String> videoIds, String userEmail) {
        Map<String, EngagementSummary> result = new HashMap<>();
        if (videoIds == null || videoIds.isEmpty()) {
            return result;
        }
        for (String id : videoIds) {
            result.put(id, emptySummary(id));
        }
        for (Object[] row : favoriteRepository.countByVideoIds(videoIds)) {
            String id = (String) row[0];
            long count = ((Number) row[1]).longValue();
            EngagementSummary prev = result.get(id);
            result.put(id, new EngagementSummary(
                    id, count, prev.commentCount(), null, prev.likeCount(), prev.dislikeCount(), null));
        }
        for (Object[] row : commentRepository.countByVideoIds(videoIds)) {
            String id = (String) row[0];
            long count = ((Number) row[1]).longValue();
            EngagementSummary prev = result.get(id);
            result.put(id, new EngagementSummary(
                    id, prev.favoriteCount(), count, null, prev.likeCount(), prev.dislikeCount(), null));
        }
        if (userEmail != null && !userEmail.isBlank()) {
            for (String id : videoIds) {
                EngagementSummary prev = result.get(id);
                Boolean favorited = favoriteRepository.existsByUserEmailAndVideoId(userEmail, id);
                result.put(id, new EngagementSummary(
                        id, prev.favoriteCount(), prev.commentCount(), favorited,
                        prev.likeCount(), prev.dislikeCount(), prev.myReaction()));
            }
        }
        return result;
    }

    private EngagementSummary summarizeOne(String videoId, String userEmail) {
        long favoriteCount = favoriteRepository.countByVideoId(videoId);
        long commentCount = commentRepository.countByVideoId(videoId);
        long[] rx = reactionCountsSafe(videoId);
        Boolean favorited = null;
        String myReaction = null;
        if (userEmail != null && !userEmail.isBlank()) {
            favorited = favoriteRepository.existsByUserEmailAndVideoId(userEmail, videoId);
            myReaction = findMyReactionSafe(userEmail, videoId);
        }
        return new EngagementSummary(videoId, favoriteCount, commentCount, favorited, rx[0], rx[1], myReaction);
    }

    private long[] reactionCountsSafe(String videoId) {
        try {
            return reactionCounts(videoId);
        } catch (RuntimeException ex) {
            return new long[]{0, 0};
        }
    }

    private String findMyReactionSafe(String userEmail, String videoId) {
        try {
            return reactionRepository.findByUserEmailAndVideoId(userEmail, videoId)
                    .map(LiveVodReactionJpaEntity::getReactionType)
                    .orElse(null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private long[] reactionCounts(String videoId) {
        long likes = 0;
        long dislikes = 0;
        for (Object[] row : reactionRepository.countGroupedByType(videoId)) {
            String type = (String) row[0];
            long count = ((Number) row[1]).longValue();
            if (REACTION_LIKE.equals(type)) {
                likes = count;
            } else if (REACTION_DISLIKE.equals(type)) {
                dislikes = count;
            }
        }
        return new long[]{likes, dislikes};
    }

    private static EngagementSummary emptySummary(String videoId) {
        return new EngagementSummary(videoId, 0, 0, null, 0, 0, null);
    }

    private static String normalizeReaction(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reaction이 필요합니다.");
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if (!REACTION_LIKE.equals(value) && !REACTION_DISLIKE.equals(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reaction은 LIKE 또는 DISLIKE 여야 합니다.");
        }
        return value;
    }

    private static String normalizeVideoId(String videoId) {
        if (videoId == null || videoId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "videoId가 필요합니다.");
        }
        String id = videoId.trim();
        if (!id.matches("[A-Za-z0-9_-]{6,32}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 videoId 입니다.");
        }
        return id;
    }

    private static void requireUser(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "user";
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
