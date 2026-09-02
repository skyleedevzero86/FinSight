package com.sleekydz86.finsight.core.media.livevod.service;

import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodCommentJpaEntity;
import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodCommentJpaRepository;
import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodCommentReactionJpaEntity;
import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodCommentReactionJpaRepository;
import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodFavoriteJpaEntity;
import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodFavoriteJpaRepository;
import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodReactionJpaEntity;
import com.sleekydz86.finsight.core.media.livevod.adapter.persistence.LiveVodReactionJpaRepository;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.CommentCreateRequest;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.CommentPageResponse;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.CommentResponse;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.EngagementSummary;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.FavoriteToggleResponse;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.LiveVodMetaResponse;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.ReactionToggleResponse;
import com.sleekydz86.finsight.core.media.livevod.domain.dto.LiveVodEngagementDtos.ReplyPageResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeVideoMeta;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.LiveVodFeedResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.out.YoutubeVideoMetaPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LiveVodEngagementService {

    private static final Logger log = LoggerFactory.getLogger(LiveVodEngagementService.class);

    public static final String REACTION_LIKE = "LIKE";
    public static final String REACTION_DISLIKE = "DISLIKE";

    private final LiveVodFavoriteJpaRepository favoriteRepository;
    private final LiveVodCommentJpaRepository commentRepository;
    private final LiveVodReactionJpaRepository reactionRepository;
    private final LiveVodCommentReactionJpaRepository commentReactionRepository;
    private final YoutubeVideoMetaPersistencePort youtubeVideoMetaPersistencePort;
    private final RestTemplate restTemplate;

    public LiveVodEngagementService(
            LiveVodFavoriteJpaRepository favoriteRepository,
            LiveVodCommentJpaRepository commentRepository,
            LiveVodReactionJpaRepository reactionRepository,
            LiveVodCommentReactionJpaRepository commentReactionRepository,
            YoutubeVideoMetaPersistencePort youtubeVideoMetaPersistencePort,
            RestTemplate restTemplate) {
        this.favoriteRepository = favoriteRepository;
        this.commentRepository = commentRepository;
        this.reactionRepository = reactionRepository;
        this.commentReactionRepository = commentReactionRepository;
        this.youtubeVideoMetaPersistencePort = youtubeVideoMetaPersistencePort;
        this.restTemplate = restTemplate;
    }

    /**
     * Adds favorite and comment counts to the videos in a live/VOD feed.
     *
     * @param feed the feed whose video items should be enriched
     * @return the original feed when it is null or has no sections; otherwise, a feed with engagement counts applied
     */
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

    /**
     * Retrieves the engagement summary for a video.
     *
     * @param videoId   the video identifier
     * @param userEmail the email address of the user viewing the video
     * @return the video's engagement summary
     */
    @Transactional(readOnly = true)
    public EngagementSummary getEngagement(String videoId, String userEmail) {
        return summarizeOne(normalizeVideoId(videoId), userEmail);
    }

    private static final String PLACEHOLDER_TITLE = "VOD 상세";

    /**
     * Resolves metadata for a live or VOD video.
     *
     * <p>Uses stored metadata when available, supplements it with YouTube oEmbed
     * metadata when necessary, and falls back to generated metadata if no usable
     * title can be obtained. Invalid video IDs also receive fallback metadata.</p>
     *
     * @param videoId the YouTube video identifier
     * @return the resolved video metadata
     */
    @Transactional(readOnly = true)
    public LiveVodMetaResponse getMeta(String videoId) {
        String id;
        try {
            id = normalizeVideoId(videoId);
        } catch (ResponseStatusException ex) {
            String raw = videoId == null ? "" : videoId.trim();
            return fallbackMeta(raw.isBlank() ? "unknown" : raw);
        }

        String watchUrl = "https://www.youtube.com/watch?v=" + id;
        String embedUrl = "https://www.youtube-nocookie.com/embed/" + id;
        String thumbnailUrl = "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg";

        LiveVodMetaResponse storedMeta = null;
        try {
            Optional<YoutubeVideoMeta> stored = youtubeVideoMetaPersistencePort.findByVideoId(id);
            if (stored.isPresent()) {
                YoutubeVideoMeta meta = stored.get();
                String title = meta.getYoutubeTitle() != null && !meta.getYoutubeTitle().isBlank()
                        ? meta.getYoutubeTitle().trim()
                        : "";
                String channel = meta.getChannelTitle();
                String thumb = meta.getThumbnailUrl() != null && !meta.getThumbnailUrl().isBlank()
                        ? meta.getThumbnailUrl()
                        : thumbnailUrl;
                String embed = meta.getEmbedUrl() != null && !meta.getEmbedUrl().isBlank()
                        ? meta.getEmbedUrl().replace(
                        "https://www.youtube.com/embed/",
                        "https://www.youtube-nocookie.com/embed/")
                        : embedUrl;
                if (isUsableTitle(title)) {
                    return new LiveVodMetaResponse(id, title, channel, thumb, embed, watchUrl);
                }
                storedMeta = new LiveVodMetaResponse(
                        id,
                        title.isBlank() ? PLACEHOLDER_TITLE : title,
                        channel,
                        thumb,
                        embed,
                        watchUrl);
            }
        } catch (Exception ex) {
            log.warn("LIVE/VOD DB 메타 조회 실패 videoId={}: {}", id, ex.getMessage());
        }

        LiveVodMetaResponse oembed = fetchOEmbedMeta(id, watchUrl, embedUrl, thumbnailUrl);
        if (oembed != null && isUsableTitle(oembed.title())) {
            return oembed;
        }
        if (storedMeta != null && isUsableTitle(storedMeta.title())) {
            return storedMeta;
        }
        return oembed != null ? oembed : fallbackMeta(id);
    }

    /**
     * Determines whether a title contains usable, non-placeholder text.
     *
     * @param title the title to evaluate
     * @return {@code true} if the title contains non-empty text other than the placeholder title, {@code false} otherwise
     */
    private static boolean isUsableTitle(String title) {
        if (title == null) {
            return false;
        }
        String trimmed = title.trim();
        return !trimmed.isEmpty() && !PLACEHOLDER_TITLE.equals(trimmed);
    }

    /**
     * Creates fallback YouTube metadata for a video identifier.
     *
     * @param id the video identifier
     * @return metadata containing placeholder title and default YouTube URLs
     */
    private static LiveVodMetaResponse fallbackMeta(String id) {
        return new LiveVodMetaResponse(
                id,
                PLACEHOLDER_TITLE,
                null,
                "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg",
                "https://www.youtube-nocookie.com/embed/" + id,
                "https://www.youtube.com/watch?v=" + id);
    }

    /**
     * Fetches video metadata from YouTube's oEmbed endpoint.
     *
     * @param id           the video identifier
     * @param watchUrl     the YouTube watch URL used for the metadata request
     * @param embedUrl     the video's embed URL
     * @param thumbnailUrl the fallback thumbnail URL
     * @return the video metadata, or {@code null} if the response is unavailable or the request fails
     */
    @SuppressWarnings("unchecked")
    private LiveVodMetaResponse fetchOEmbedMeta(
            String id,
            String watchUrl,
            String embedUrl,
            String thumbnailUrl) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString("https://www.youtube.com/oembed")
                    .queryParam("format", "json")
                    .queryParam("url", watchUrl)
                    .encode()
                    .build()
                    .toUri();
            Map<String, Object> body = restTemplate.getForObject(uri, Map.class);
            if (body == null || body.isEmpty()) {
                return null;
            }
            Object titleObj = body.get("title");
            Object authorObj = body.get("author_name");
            Object thumbObj = body.get("thumbnail_url");
            String title = titleObj instanceof String s && !s.isBlank() ? s.trim() : PLACEHOLDER_TITLE;
            String channel = authorObj instanceof String s && !s.isBlank() ? s : null;
            String thumb = thumbObj instanceof String s && !s.isBlank() ? s : thumbnailUrl;
            return new LiveVodMetaResponse(id, title, channel, thumb, embedUrl, watchUrl);
        } catch (Exception ex) {
            log.warn("YouTube oEmbed 조회 실패 videoId={}: {}", id, ex.getMessage());
            return null;
        }
    }

    /**
     * Toggles the authenticated user's favorite status for a video.
     *
     * @param videoId   the video identifier
     * @param userEmail the authenticated user's email address
     * @return the updated favorite status and total favorite count for the video
     */
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

    /**
     * Toggles the authenticated user's reaction for a video.
     *
     * @param videoId     the video identifier
     * @param userEmail   the authenticated user's email address
     * @param reactionRaw the requested reaction, either {@code LIKE} or {@code DISLIKE}
     * @return the user's current reaction and the video's total like and dislike counts
     */
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

    /**
     * Retrieves a paginated list of top-level comments for a video, including the first page of replies and reaction details.
     *
     * @param videoId     the video identifier
     * @param page        the zero-based page number
     * @param size        the requested page size
     * @param viewerEmail the email of the viewing user, if available
     * @return the paginated comments, reply metadata, total comment counts, and page status
     */
    @Transactional(readOnly = true)
    public CommentPageResponse listComments(String videoId, int page, int size, String viewerEmail) {
        String id = normalizeVideoId(videoId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? ROOT_COMMENT_PAGE_SIZE : Math.min(size, 50);
        Page<LiveVodCommentJpaEntity> rootPage = commentRepository
                .findByVideoIdAndParentIsNullOrderByCreatedAtDesc(id, PageRequest.of(safePage, safeSize));

        List<LiveVodCommentJpaEntity> roots = rootPage.getContent();
        Map<Long, long[]> replyPagesMeta = new HashMap<>();
        Map<Long, List<LiveVodCommentJpaEntity>> firstReplies = new HashMap<>();
        List<Long> allIds = new ArrayList<>();
        for (LiveVodCommentJpaEntity root : roots) {
            allIds.add(root.getId());
            Page<LiveVodCommentJpaEntity> replies = commentRepository
                    .findByParent_IdOrderByCreatedAtAsc(root.getId(), PageRequest.of(0, REPLY_PAGE_SIZE));
            firstReplies.put(root.getId(), replies.getContent());
            replyPagesMeta.put(root.getId(), new long[]{
                    replies.getTotalElements(),
                    replies.getNumber(),
                    Math.max(replies.getTotalPages(), 0)
            });
            for (LiveVodCommentJpaEntity reply : replies.getContent()) {
                allIds.add(reply.getId());
            }
        }
        ReactionView reactionView = loadCommentReactions(allIds, viewerEmail);

        List<CommentResponse> items = roots.stream()
                .map(root -> {
                    long[] meta = replyPagesMeta.getOrDefault(root.getId(), new long[]{0, 0, 0});
                    List<CommentResponse> replyItems = firstReplies.getOrDefault(root.getId(), List.of()).stream()
                            .map(reply -> toComment(reply, List.of(), 0, 0, 0, reactionView))
                            .toList();
                    return toComment(
                            root,
                            replyItems,
                            meta[0],
                            (int) meta[1],
                            (int) meta[2],
                            reactionView);
                })
                .toList();

        return new CommentPageResponse(
                items,
                rootPage.getNumber(),
                rootPage.getSize(),
                rootPage.getTotalElements(),
                commentRepository.countByVideoId(id),
                rootPage.hasNext());
    }

    /**
     * Retrieves a paginated list of replies for a top-level comment.
     *
     * @param videoId     the video identifier associated with the parent comment
     * @param parentId    the identifier of the top-level comment
     * @param page        the zero-based page number
     * @param size        the requested page size, limited to 20 replies
     * @param viewerEmail the viewer's email for resolving the viewer's reactions
     * @return            the paginated replies and their reaction information
     */
    @Transactional(readOnly = true)
    public ReplyPageResponse listReplies(String videoId, Long parentId, int page, int size, String viewerEmail) {
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

        List<Long> ids = replyPage.getContent().stream().map(LiveVodCommentJpaEntity::getId).toList();
        ReactionView reactionView = loadCommentReactions(ids, viewerEmail);
        List<CommentResponse> items = replyPage.getContent().stream()
                .map(row -> toComment(row, List.of(), 0, 0, 0, reactionView))
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

    /**
     * Toggles the authenticated user's reaction on a comment.
     *
     * @param commentId   the identifier of the comment
     * @param userEmail   the authenticated user's email address
     * @param reactionRaw the requested reaction, either {@code LIKE} or {@code DISLIKE}
     * @return the user's current reaction and the comment's like and dislike counts
     * @throws ResponseStatusException if authentication fails, the comment ID is missing,
     *                                 the comment does not exist, or the reaction is invalid
     */
    @Transactional
    public ReactionToggleResponse toggleCommentReaction(Long commentId, String userEmail, String reactionRaw) {
        requireUser(userEmail);
        if (commentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "commentId가 필요합니다.");
        }
        LiveVodCommentJpaEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
        String reaction = normalizeReaction(reactionRaw);
        Optional<LiveVodCommentReactionJpaEntity> existing =
                commentReactionRepository.findByUserEmailAndCommentId(userEmail, comment.getId());
        String myReaction = null;
        if (existing.isPresent()) {
            LiveVodCommentReactionJpaEntity row = existing.get();
            if (reaction.equals(row.getReactionType())) {
                commentReactionRepository.delete(row);
            } else {
                row.setReactionType(reaction);
                commentReactionRepository.save(row);
                myReaction = reaction;
            }
        } else {
            commentReactionRepository.save(new LiveVodCommentReactionJpaEntity(comment.getId(), userEmail, reaction));
            myReaction = reaction;
        }
        long likes = commentReactionRepository.countByCommentIdAndReactionType(comment.getId(), REACTION_LIKE);
        long dislikes = commentReactionRepository.countByCommentIdAndReactionType(comment.getId(), REACTION_DISLIKE);
        return new ReactionToggleResponse(myReaction, likes, dislikes);
    }

    /**
     * Creates a comment or reply for a video.
     *
     * @param videoId the target video's identifier
     * @param userEmail the authenticated user's email address
     * @param nickname the author's display name
     * @param request the comment content and optional parent comment identifier
     * @return the created comment
     * @throws ResponseStatusException if the user, video, comment content, or parent comment is invalid
     */
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
                LiveVodCommentJpaEntity root = commentRepository.findById(parent.getParentId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "부모 댓글을 찾을 수 없습니다."));
                String mention = parent.getAuthorNickname() != null && !parent.getAuthorNickname().isBlank()
                        ? parent.getAuthorNickname()
                        : parent.getUserEmail();
                if (mention != null && !mention.isBlank()
                        && !content.regionMatches(true, 0, "@" + mention, 0, mention.length() + 1)) {
                    content = "@" + mention + " " + content;
                    if (content.length() > 2000) {
                        content = content.substring(0, 2000);
                    }
                }
                parent = root;
            }
        }

        LiveVodCommentJpaEntity saved = commentRepository.save(
                new LiveVodCommentJpaEntity(id, userEmail, nickname, content, parent));
        ReactionView empty = ReactionView.empty();
        if (parent == null) {
            return toComment(saved, List.of(), 0, 0, 0, empty);
        }
        return toComment(saved, List.of(), 0, 0, 0, empty);
    }

    /**
     * Converts a comment entity and its engagement data into a comment response.
     *
     * @param row              the comment entity
     * @param replies          the comment's replies
     * @param replyCount       the total number of replies
     * @param replyPage        the current reply page
     * @param replyTotalPages  the total number of reply pages
     * @param reactionView     the comment reaction counts and current user's reactions
     * @return                the comment response with masked author email and engagement data
     */
    private CommentResponse toComment(
            LiveVodCommentJpaEntity row,
            List<CommentResponse> replies,
            long replyCount,
            int replyPage,
            int replyTotalPages,
            ReactionView reactionView) {
        long[] counts = reactionView.counts.getOrDefault(row.getId(), new long[]{0, 0});
        String mine = reactionView.mine.get(row.getId());
        return new CommentResponse(
                row.getId(),
                row.getVideoId(),
                maskEmail(row.getUserEmail()),
                row.getAuthorNickname(),
                row.getContent(),
                row.getParentId(),
                row.getCreatedAt(),
                replies,
                replyCount,
                replyPage,
                replyTotalPages,
                counts[0],
                counts[1],
                mine);
    }

    /**
     * Loads reaction counts and the viewer's reactions for the specified comments.
     *
     * @param commentIds   the comment identifiers to include
     * @param viewerEmail  the email address of the viewer, or {@code null} to omit viewer-specific reactions
     * @return             reaction counts and viewer reactions keyed by comment identifier
     */
    private ReactionView loadCommentReactions(Collection<Long> commentIds, String viewerEmail) {
        if (commentIds == null || commentIds.isEmpty()) {
            return ReactionView.empty();
        }
        Set<Long> ids = new HashSet<>(commentIds);
        Map<Long, long[]> counts = new HashMap<>();
        for (Long id : ids) {
            counts.put(id, new long[]{0, 0});
        }
        try {
            for (Object[] row : commentReactionRepository.countGroupedByCommentIds(ids)) {
                Long commentId = ((Number) row[0]).longValue();
                String type = (String) row[1];
                long count = ((Number) row[2]).longValue();
                long[] bucket = counts.computeIfAbsent(commentId, ignored -> new long[]{0, 0});
                if (REACTION_LIKE.equals(type)) {
                    bucket[0] = count;
                } else if (REACTION_DISLIKE.equals(type)) {
                    bucket[1] = count;
                }
            }
        } catch (RuntimeException ex) {
            log.warn("댓글 반응 집계 실패: {}", ex.getMessage());
        }

        Map<Long, String> mine = new HashMap<>();
        if (viewerEmail != null && !viewerEmail.isBlank()) {
            try {
                for (LiveVodCommentReactionJpaEntity row :
                        commentReactionRepository.findByUserEmailAndCommentIdIn(viewerEmail, ids)) {
                    mine.put(row.getCommentId(), row.getReactionType());
                }
            } catch (RuntimeException ex) {
                log.warn("내 댓글 반응 조회 실패: {}", ex.getMessage());
            }
        }
        return new ReactionView(counts, mine);
    }

    private record ReactionView(Map<Long, long[]> counts, Map<Long, String> mine) {
        static ReactionView empty() {
            return new ReactionView(Map.of(), Map.of());
        }
    }

    /**
     * Applies engagement counts to a live or VOD feed item.
     *
     * @param item the feed item to enrich
     * @param map  engagement summaries keyed by video ID
     * @return the feed item with its favorite and comment counts
     */
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

    /**
     * Builds engagement summaries for multiple videos.
     *
     * @param videoIds  the video IDs to summarize
     * @param userEmail the viewer's email for determining favorite status, or {@code null} to omit it
     * @return a summary for each requested video ID
     */
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

    /**
     * Builds an engagement summary for a video, optionally including the user's favorite status and reaction.
     *
     * @param videoId   the video identifier
     * @param userEmail the user's email, or {@code null} or blank when no user context is available
     * @return the video's favorite, comment, like, dislike, and user-specific engagement details
     */
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

    /**
     * Retrieves the user's current reaction for a video.
     *
     * @param userEmail the user's email address
     * @param videoId   the video identifier
     * @return the user's reaction type, or {@code null} if no reaction exists or the lookup fails
     */
    private String findMyReactionSafe(String userEmail, String videoId) {
        try {
            return reactionRepository.findByUserEmailAndVideoId(userEmail, videoId)
                    .map(LiveVodReactionJpaEntity::getReactionType)
                    .orElse(null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * Counts the likes and dislikes for a video.
     *
     * @param videoId the video identifier
     * @return an array containing the like count at index 0 and the dislike count at index 1
     */
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

    /**
     * Creates an empty engagement summary for a video.
     *
     * @param videoId the video identifier
     * @return an engagement summary with zero counts and no user-specific state
     */
    private static EngagementSummary emptySummary(String videoId) {
        return new EngagementSummary(videoId, 0, 0, null, 0, 0, null);
    }

    /**
     * Normalizes a reaction value and validates that it is supported.
     *
     * @param raw the reaction value to normalize
     * @return the trimmed, uppercase reaction value
     */
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

    /**
     * Validates and trims a video identifier.
     *
     * @param videoId the identifier to validate
     * @return the trimmed video identifier
     * @throws ResponseStatusException if the identifier is blank or does not contain 6 to 32 letters, digits, underscores, or hyphens
     */
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
