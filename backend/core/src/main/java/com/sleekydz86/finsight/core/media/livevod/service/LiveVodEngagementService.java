package com.sleekydz86.finsight.core.media.livevod.service;

import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodCommentJpaEntity;
import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodCommentJpaRepository;
import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodFavoriteJpaEntity;
import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodFavoriteJpaRepository;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.CommentCreateRequest;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.CommentResponse;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.EngagementSummary;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.FavoriteToggleResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.LiveVodFeedResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LiveVodEngagementService {

    private final LiveVodFavoriteJpaRepository favoriteRepository;
    private final LiveVodCommentJpaRepository commentRepository;

    public LiveVodEngagementService(
            LiveVodFavoriteJpaRepository favoriteRepository,
            LiveVodCommentJpaRepository commentRepository) {
        this.favoriteRepository = favoriteRepository;
        this.commentRepository = commentRepository;
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
        boolean exists = favoriteRepository.existsByUserEmailAndVideoId(userEmail, id);
        if (exists) {
            favoriteRepository.deleteByUserEmailAndVideoId(userEmail, id);
        } else {
            favoriteRepository.save(new LiveVodFavoriteJpaEntity(id, userEmail));
        }
        return new FavoriteToggleResponse(!exists, favoriteRepository.countByVideoId(id));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(String videoId) {
        String id = normalizeVideoId(videoId);
        List<LiveVodCommentJpaEntity> all = commentRepository.findByVideoIdOrderByCreatedAtAsc(id);
        Map<Long, CommentResponse> byId = new LinkedHashMap<>();
        Map<Long, List<CommentResponse>> children = new HashMap<>();

        for (LiveVodCommentJpaEntity row : all) {
            CommentResponse node = new CommentResponse(
                    row.getId(),
                    row.getVideoId(),
                    maskEmail(row.getUserEmail()),
                    row.getAuthorNickname(),
                    row.getContent(),
                    row.getParentId(),
                    row.getCreatedAt(),
                    new ArrayList<>());
            byId.put(row.getId(), node);
            if (row.getParentId() != null) {
                children.computeIfAbsent(row.getParentId(), ignored -> new ArrayList<>()).add(node);
            }
        }

        List<CommentResponse> roots = new ArrayList<>();
        for (CommentResponse node : byId.values()) {
            if (node.parentId() == null) {
                List<CommentResponse> replies = children.getOrDefault(node.id(), List.of());
                roots.add(new CommentResponse(
                        node.id(),
                        node.videoId(),
                        node.userEmail(),
                        node.authorNickname(),
                        node.content(),
                        node.parentId(),
                        node.createdAt(),
                        replies));
            }
        }
        return roots;
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
        return new CommentResponse(
                saved.getId(),
                saved.getVideoId(),
                maskEmail(saved.getUserEmail()),
                saved.getAuthorNickname(),
                saved.getContent(),
                saved.getParentId(),
                saved.getCreatedAt(),
                List.of());
    }

    private LiveVodFeedResponse.LiveVodItemResponse withCounts(
            LiveVodFeedResponse.LiveVodItemResponse item,
            Map<String, EngagementSummary> map) {
        EngagementSummary summary = map.getOrDefault(
                item.videoId(),
                new EngagementSummary(item.videoId(), 0, 0, null));
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
            result.put(id, new EngagementSummary(id, 0, 0, null));
        }
        for (Object[] row : favoriteRepository.countByVideoIds(videoIds)) {
            String id = (String) row[0];
            long count = ((Number) row[1]).longValue();
            EngagementSummary prev = result.get(id);
            result.put(id, new EngagementSummary(id, count, prev.commentCount(), null));
        }
        for (Object[] row : commentRepository.countByVideoIds(videoIds)) {
            String id = (String) row[0];
            long count = ((Number) row[1]).longValue();
            EngagementSummary prev = result.get(id);
            result.put(id, new EngagementSummary(id, prev.favoriteCount(), count, null));
        }
        if (userEmail != null && !userEmail.isBlank()) {
            for (String id : videoIds) {
                EngagementSummary prev = result.get(id);
                Boolean favorited = favoriteRepository.existsByUserEmailAndVideoId(userEmail, id);
                result.put(id, new EngagementSummary(
                        id, prev.favoriteCount(), prev.commentCount(), favorited));
            }
        }
        return result;
    }

    private EngagementSummary summarizeOne(String videoId, String userEmail) {
        long favoriteCount = favoriteRepository.countByVideoId(videoId);
        long commentCount = commentRepository.countByVideoId(videoId);
        Boolean favorited = null;
        if (userEmail != null && !userEmail.isBlank()) {
            favorited = favoriteRepository.existsByUserEmailAndVideoId(userEmail, videoId);
        }
        return new EngagementSummary(videoId, favoriteCount, commentCount, favorited);
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
