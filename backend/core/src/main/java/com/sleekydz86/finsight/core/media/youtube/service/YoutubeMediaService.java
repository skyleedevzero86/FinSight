package com.sleekydz86.finsight.core.media.youtube.service;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardNavigationResponse;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardPersistencePort;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.exception.BoardNotFoundException;
import com.sleekydz86.finsight.core.media.youtube.adapter.requester.YoutubeAiContentRequester;
import com.sleekydz86.finsight.core.media.youtube.adapter.requester.YoutubeApiClient;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeGeneratedContent;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSource;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSourceType;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportStatus;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeVideoMeta;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.YoutubeMediaAdminUseCase;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.YoutubeMediaImportUseCase;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.YoutubeMediaQueryUseCase;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeAdminVideoSearchRequest;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeAiEnrichmentSummaryResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeImportSourceCreateRequest;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeImportSourceResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeManualImportRequest;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeSourceReviewRequest;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeSourceReviewResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeSyncSummaryResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoDetailResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoListResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoPublishRequest;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeVideoSearchRequest;
import com.sleekydz86.finsight.core.media.youtube.domain.port.out.YoutubeImportSourcePersistencePort;
import com.sleekydz86.finsight.core.media.youtube.domain.port.out.YoutubeVideoMetaPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class YoutubeMediaService implements YoutubeMediaQueryUseCase, YoutubeMediaAdminUseCase, YoutubeMediaImportUseCase {

    private static final Logger log = LoggerFactory.getLogger(YoutubeMediaService.class);
    private static final String SYSTEM_AUTHOR_EMAIL = "media-batch@finsight.local";
    private static final int TITLE_MAX_LENGTH = 200;
    private static final int CONTENT_MAX_LENGTH = 10000;
    private static final int PREVIEW_MAX_LENGTH = 180;

    private final YoutubeVideoMetaPersistencePort youtubeVideoMetaPersistencePort;
    private final YoutubeImportSourcePersistencePort youtubeImportSourcePersistencePort;
    private final BoardPersistencePort boardPersistencePort;
    private final YoutubeApiClient youtubeApiClient;
    private final YoutubeAiContentRequester youtubeAiContentRequester;

    @Value("${youtube.ai.batch-size:10}")
    private int aiBatchSize;

    public YoutubeMediaService(
            YoutubeVideoMetaPersistencePort youtubeVideoMetaPersistencePort,
            YoutubeImportSourcePersistencePort youtubeImportSourcePersistencePort,
            BoardPersistencePort boardPersistencePort,
            YoutubeApiClient youtubeApiClient,
            YoutubeAiContentRequester youtubeAiContentRequester) {
        this.youtubeVideoMetaPersistencePort = youtubeVideoMetaPersistencePort;
        this.youtubeImportSourcePersistencePort = youtubeImportSourcePersistencePort;
        this.boardPersistencePort = boardPersistencePort;
        this.youtubeApiClient = youtubeApiClient;
        this.youtubeAiContentRequester = youtubeAiContentRequester;
    }

    @Override
    public PaginationResponse<YoutubeVideoListResponse> getPublishedVideos(YoutubeVideoSearchRequest request) {
        PageRequest pageable = PageRequest.of(safePage(request.getPage()), safeSize(request.getSize()));
        Page<YoutubeVideoMeta> page = youtubeVideoMetaPersistencePort.search(
                YoutubeImportStatus.PUBLISHED,
                normalizeText(request.getCategory()),
                pageable);

        return toPaginationResponse(page);
    }

    @Override
    public YoutubeVideoDetailResponse getPublishedVideoDetail(Long boardId) {
        return getVideoDetail(boardId, true);
    }

    @Override
    public PaginationResponse<YoutubeVideoListResponse> getAdminVideos(YoutubeAdminVideoSearchRequest request) {
        PageRequest pageable = PageRequest.of(safePage(request.getPage()), safeSize(request.getSize()));
        Page<YoutubeVideoMeta> page = youtubeVideoMetaPersistencePort.search(
                parseImportStatus(request.getImportStatus()),
                normalizeText(request.getCategory()),
                pageable);

        return toPaginationResponse(page);
    }

    @Override
    public YoutubeVideoDetailResponse getAdminVideoDetail(Long boardId) {
        return getVideoDetail(boardId, false);
    }

    @Override
    public List<YoutubeImportSourceResponse> getImportSources() {
        return youtubeImportSourcePersistencePort.findAll().stream()
                .map(this::toSourceResponse)
                .toList();
    }

    @Override
    public YoutubeSourceReviewResponse getSourceReview(Long sourceId, YoutubeSourceReviewRequest request) {
        YoutubeImportSource source = youtubeImportSourcePersistencePort.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("YouTube import source not found: " + sourceId));

        PageRequest pageable = PageRequest.of(safePage(request.getPage()), safeSize(request.getSize()));
        Page<YoutubeVideoMeta> page = youtubeVideoMetaPersistencePort.searchBySource(
                source.getSourceType(),
                source.getSourceValue(),
                parseImportStatus(request.getImportStatus()),
                pageable);

        return YoutubeSourceReviewResponse.builder()
                .sourceId(source.getId())
                .sourceType(source.getSourceType())
                .sourceValue(source.getSourceValue())
                .category(source.getCategory())
                .active(source.isActive())
                .autoPublish(source.isAutoPublish())
                .lastSyncedAt(source.getLastSyncedAt())
                .totalVideoCount(youtubeVideoMetaPersistencePort.countBySource(source.getSourceType(), source.getSourceValue()))
                .draftVideoCount(youtubeVideoMetaPersistencePort.countBySourceAndImportStatus(
                        source.getSourceType(),
                        source.getSourceValue(),
                        YoutubeImportStatus.DRAFT))
                .publishedVideoCount(youtubeVideoMetaPersistencePort.countBySourceAndImportStatus(
                        source.getSourceType(),
                        source.getSourceValue(),
                        YoutubeImportStatus.PUBLISHED))
                .hiddenVideoCount(youtubeVideoMetaPersistencePort.countBySourceAndImportStatus(
                        source.getSourceType(),
                        source.getSourceValue(),
                        YoutubeImportStatus.HIDDEN))
                .pendingAiCount(youtubeVideoMetaPersistencePort.countPendingAiBySource(
                        source.getSourceType(),
                        source.getSourceValue()))
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .videos(toPaginationResponse(page))
                .build();
    }

    @Override
    @Transactional
    public YoutubeImportSourceResponse createImportSource(String adminEmail, YoutubeImportSourceCreateRequest request) {
        YoutubeImportSource source = YoutubeImportSource.builder()
                .sourceType(request.getSourceType())
                .sourceValue(request.getSourceValue().trim())
                .category(normalizeText(request.getCategory()))
                .active(request.isActive())
                .autoPublish(request.isAutoPublish())
                .build();

        YoutubeImportSource savedSource = youtubeImportSourcePersistencePort.save(source);
        log.info("Created YouTube import source {} by {}", savedSource.getId(), adminEmail);
        return toSourceResponse(savedSource);
    }

    @Override
    @Transactional
    public YoutubeSyncSummaryResponse importManualUrls(String adminEmail, YoutubeManualImportRequest request) {
        List<String> urls = request.getUrls() != null ? request.getUrls() : List.of();
        List<YoutubeApiClient.FetchedYoutubeVideo> fetchedVideos = youtubeApiClient.fetchVideosByUrls(
                urls,
                normalizeText(request.getCategory()),
                YoutubeImportSourceType.MANUAL_URL,
                "manual");

        YoutubeSyncSummaryResponse summary = upsertVideos(
                fetchedVideos,
                request.isAutoPublish(),
                adminEmail,
                normalizeHashtags(request.getHashtags(), request.getCategory()),
                0);

        return YoutubeSyncSummaryResponse.builder()
                .sourceCount(summary.getSourceCount())
                .requestedCount(urls.size())
                .fetchedCount(summary.getFetchedCount())
                .importedCount(summary.getImportedCount())
                .updatedCount(summary.getUpdatedCount())
                .hiddenCount(summary.getHiddenCount())
                .skippedCount(Math.max(urls.size() - summary.getFetchedCount(), 0) + summary.getSkippedCount())
                .failedCount(summary.getFailedCount())
                .build();
    }

    @Override
    @Transactional
    public YoutubeSyncSummaryResponse syncSource(Long sourceId) {
        YoutubeImportSource source = youtubeImportSourcePersistencePort.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("YouTube import source not found: " + sourceId));

        YoutubeSyncSummaryResponse summary = syncSingleSource(source);
        log.info("Synced YouTube import source {} with imported={}, updated={}",
                sourceId, summary.getImportedCount(), summary.getUpdatedCount());
        return summary;
    }

    @Override
    @Transactional
    public YoutubeVideoDetailResponse publishVideo(Long boardId, String adminEmail, YoutubeVideoPublishRequest request) {
        Board board = loadBoard(boardId);
        YoutubeVideoMeta meta = loadVideoMetaByBoardId(boardId);

        Board publishedBoard = rebuildBoard(
                board,
                trimToLength(request.getTitle(), TITLE_MAX_LENGTH),
                trimToLength(request.getContent(), CONTENT_MAX_LENGTH),
                normalizeHashtags(request.getHashtags(), meta.getCategory()),
                BoardStatus.ACTIVE,
                adminEmail);

        boardPersistencePort.save(publishedBoard);
        youtubeVideoMetaPersistencePort.save(rebuildMeta(meta, publishedBoard.getId(), YoutubeImportStatus.PUBLISHED));

        return getVideoDetail(boardId, false);
    }

    @Override
    @Transactional
    public YoutubeVideoDetailResponse hideVideo(Long boardId) {
        Board board = loadBoard(boardId);
        YoutubeVideoMeta meta = loadVideoMetaByBoardId(boardId);

        Board hiddenBoard = rebuildBoard(
                board,
                board.getTitle(),
                board.getContent(),
                board.getHashtags(),
                BoardStatus.HIDDEN,
                board.getAuthorEmail());

        boardPersistencePort.save(hiddenBoard);
        youtubeVideoMetaPersistencePort.save(rebuildMeta(meta, hiddenBoard.getId(), YoutubeImportStatus.HIDDEN));

        return getVideoDetail(boardId, false);
    }

    @Override
    @Transactional
    public YoutubeSyncSummaryResponse syncActiveSources() {
        YoutubeSyncSummaryResponse total = emptySummary();
        for (YoutubeImportSource source : youtubeImportSourcePersistencePort.findActiveSources()) {
            total = total.merge(syncSingleSource(source));
        }
        return total;
    }

    @Override
    @Transactional
    public YoutubeAiEnrichmentSummaryResponse enrichPendingDraftVideos() {
        List<YoutubeVideoMeta> pendingVideos = youtubeVideoMetaPersistencePort.findPendingAiEnrichment(aiBatchSize);
        YoutubeAiEnrichmentSummaryResponse summary = YoutubeAiEnrichmentSummaryResponse.builder()
                .requestedCount(pendingVideos.size())
                .enrichedCount(0)
                .skippedCount(0)
                .failedCount(0)
                .build();

        for (YoutubeVideoMeta meta : pendingVideos) {
            try {
                Board board = loadBoard(meta.getBoardId());
                if (board.getStatus() != BoardStatus.DRAFT || meta.getImportStatus() != YoutubeImportStatus.DRAFT) {
                    summary = summary.merge(YoutubeAiEnrichmentSummaryResponse.builder().skippedCount(1).build());
                    continue;
                }

                YoutubeGeneratedContent generatedContent = youtubeAiContentRequester.generate(meta, board);
                youtubeVideoMetaPersistencePort.save(enrichMeta(meta, generatedContent));
                summary = summary.merge(YoutubeAiEnrichmentSummaryResponse.builder().enrichedCount(1).build());
            } catch (Exception e) {
                log.error("Failed to enrich YouTube draft board {}", meta.getBoardId(), e);
                summary = summary.merge(YoutubeAiEnrichmentSummaryResponse.builder().failedCount(1).build());
            }
        }

        return summary;
    }

    private YoutubeSyncSummaryResponse syncSingleSource(YoutubeImportSource source) {
        List<YoutubeApiClient.FetchedYoutubeVideo> fetchedVideos = youtubeApiClient.fetchBySource(source);
        YoutubeSyncSummaryResponse summary = upsertVideos(
                fetchedVideos,
                source.isAutoPublish(),
                SYSTEM_AUTHOR_EMAIL,
                normalizeHashtags(List.of(), source.getCategory()),
                1);

        youtubeImportSourcePersistencePort.save(YoutubeImportSource.builder()
                .id(source.getId())
                .sourceType(source.getSourceType())
                .sourceValue(source.getSourceValue())
                .category(source.getCategory())
                .active(source.isActive())
                .autoPublish(source.isAutoPublish())
                .lastSyncedAt(LocalDateTime.now())
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .build());

        return YoutubeSyncSummaryResponse.builder()
                .sourceCount(summary.getSourceCount())
                .requestedCount(summary.getFetchedCount())
                .fetchedCount(summary.getFetchedCount())
                .importedCount(summary.getImportedCount())
                .updatedCount(summary.getUpdatedCount())
                .hiddenCount(summary.getHiddenCount())
                .skippedCount(summary.getSkippedCount())
                .failedCount(summary.getFailedCount())
                .build();
    }

    private YoutubeSyncSummaryResponse upsertVideos(
            List<YoutubeApiClient.FetchedYoutubeVideo> fetchedVideos,
            boolean autoPublish,
            String authorEmail,
            List<String> hashtags,
            int sourceCount) {

        YoutubeSyncSummaryResponse summary = YoutubeSyncSummaryResponse.builder()
                .sourceCount(sourceCount)
                .requestedCount(fetchedVideos.size())
                .fetchedCount(fetchedVideos.size())
                .build();

        for (YoutubeApiClient.FetchedYoutubeVideo fetchedVideo : fetchedVideos) {
            try {
                Optional<YoutubeVideoMeta> existingMeta = youtubeVideoMetaPersistencePort.findByVideoId(fetchedVideo.getVideoId());
                if (existingMeta.isPresent()) {
                    YoutubeVideoMeta currentMeta = existingMeta.get();
                    Board currentBoard = loadBoard(currentMeta.getBoardId());

                    Board updatedBoard = rebuildBoard(
                            currentBoard,
                            currentBoard.getTitle(),
                            currentBoard.getContent(),
                            currentBoard.getHashtags(),
                            currentBoard.getStatus(),
                            currentBoard.getAuthorEmail());

                    boardPersistencePort.save(updatedBoard);
                    youtubeVideoMetaPersistencePort.save(toVideoMeta(
                            currentMeta,
                            updatedBoard.getId(),
                            fetchedVideo,
                            currentMeta.getCategory(),
                            toImportStatus(currentBoard.getStatus()),
                            currentMeta.getCreatedAt()));

                    summary = summary.merge(YoutubeSyncSummaryResponse.builder().updatedCount(1).build());
                } else {
                    Board savedBoard = boardPersistencePort.save(Board.builder()
                            .title(trimToLength(fetchedVideo.getYoutubeTitle(), TITLE_MAX_LENGTH))
                            .content(trimToLength(defaultContent(fetchedVideo.getYoutubeDescription()), CONTENT_MAX_LENGTH))
                            .authorEmail(authorEmail)
                            .boardType(BoardType.MEDIA)
                            .status(BoardStatus.DRAFT)
                            .hashtags(hashtags)
                            .build());

                    youtubeVideoMetaPersistencePort.save(toVideoMeta(
                            null,
                            savedBoard.getId(),
                            fetchedVideo,
                            normalizeText(fetchedVideo.getCategory()),
                            YoutubeImportStatus.DRAFT,
                            null));

                    summary = summary.merge(YoutubeSyncSummaryResponse.builder().importedCount(1).build());
                }
            } catch (Exception e) {
                log.error("Failed to import YouTube video {}", fetchedVideo.getVideoId(), e);
                summary = summary.merge(YoutubeSyncSummaryResponse.builder().failedCount(1).build());
            }
        }

        return summary;
    }

    private PaginationResponse<YoutubeVideoListResponse> toPaginationResponse(Page<YoutubeVideoMeta> page) {
        List<Long> boardIds = page.getContent().stream()
                .map(YoutubeVideoMeta::getBoardId)
                .toList();

        Map<Long, Board> boardMap = boardIds.stream()
                .map(boardPersistencePort::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Board::getId, Function.identity()));

        List<YoutubeVideoListResponse> responses = page.getContent().stream()
                .map(meta -> toListResponse(boardMap.get(meta.getBoardId()), meta))
                .filter(Objects::nonNull)
                .toList();

        return PaginationResponse.<YoutubeVideoListResponse>builder()
                .content(responses)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .build();
    }

    private YoutubeVideoListResponse toListResponse(Board board, YoutubeVideoMeta meta) {
        if (board == null) {
            return null;
        }

        return YoutubeVideoListResponse.builder()
                .boardId(board.getId())
                .title(board.getTitle())
                .previewContent(toPreview(firstNonBlank(meta.getSummary(), board.getContent(), meta.getYoutubeDescription())))
                .authorEmail(board.getAuthorEmail())
                .boardStatus(board.getStatus())
                .videoId(meta.getVideoId())
                .channelId(meta.getChannelId())
                .channelTitle(meta.getChannelTitle())
                .sourceType(meta.getSourceType())
                .sourceValue(meta.getSourceValue())
                .category(meta.getCategory())
                .thumbnailUrl(meta.getThumbnailUrl())
                .publishedAt(meta.getPublishedAt())
                .duration(meta.getDuration())
                .summary(meta.getSummary())
                .importStatus(meta.getImportStatus())
                .hashtags(board.getHashtags())
                .aiGeneratedAt(meta.getAiGeneratedAt())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }

    private YoutubeVideoDetailResponse getVideoDetail(Long boardId, boolean publishedOnly) {
        Board board = loadBoard(boardId);
        YoutubeVideoMeta meta = loadVideoMetaByBoardId(boardId);

        if (publishedOnly && (board.getStatus() != BoardStatus.ACTIVE || meta.getImportStatus() != YoutubeImportStatus.PUBLISHED)) {
            throw new BoardNotFoundException(boardId);
        }

        Board resolvedBoard = board;
        if (publishedOnly) {
            boardPersistencePort.incrementViewCount(boardId);
            resolvedBoard = board.incrementView();
        }

        return YoutubeVideoDetailResponse.builder()
                .boardId(resolvedBoard.getId())
                .title(resolvedBoard.getTitle())
                .content(resolvedBoard.getContent())
                .authorEmail(resolvedBoard.getAuthorEmail())
                .boardType(resolvedBoard.getBoardType())
                .boardStatus(resolvedBoard.getStatus())
                .viewCount(resolvedBoard.getViewCount())
                .likeCount(resolvedBoard.getLikeCount())
                .dislikeCount(resolvedBoard.getDislikeCount())
                .commentCount(resolvedBoard.getCommentCount())
                .reportCount(resolvedBoard.getReportCount())
                .hashtags(resolvedBoard.getHashtags())
                .videoId(meta.getVideoId())
                .channelId(meta.getChannelId())
                .channelTitle(meta.getChannelTitle())
                .sourceType(meta.getSourceType())
                .sourceValue(meta.getSourceValue())
                .category(meta.getCategory())
                .youtubeTitle(meta.getYoutubeTitle())
                .youtubeDescription(meta.getYoutubeDescription())
                .thumbnailUrl(meta.getThumbnailUrl())
                .embedUrl(meta.getEmbedUrl())
                .publishedAt(meta.getPublishedAt())
                .duration(meta.getDuration())
                .summary(meta.getSummary())
                .editorComment(meta.getEditorComment())
                .keyPoints(meta.getKeyPoints())
                .aiGeneratedAt(meta.getAiGeneratedAt())
                .importStatus(meta.getImportStatus())
                .syncedAt(meta.getSyncedAt())
                .createdAt(resolvedBoard.getCreatedAt())
                .updatedAt(resolvedBoard.getUpdatedAt())
                .navigation(buildNavigation(boardId))
                .build();
    }

    private BoardNavigationResponse buildNavigation(Long boardId) {
        List<Board> navigationBoards = boardPersistencePort.findPreviousAndNext(boardId, BoardType.MEDIA);
        if (navigationBoards.isEmpty()) {
            return null;
        }

        Board previous = null;
        Board next = null;
        for (Board board : navigationBoards) {
            if (board.getId() < boardId) {
                previous = board;
            } else if (board.getId() > boardId) {
                next = board;
            }
        }

        return new BoardNavigationResponse(
                previous != null
                        ? new BoardNavigationResponse.BoardNavigationItem(
                        previous.getId(),
                        previous.getTitle(),
                        previous.getAuthorEmail(),
                        previous.getCreatedAt().toString())
                        : null,
                next != null
                        ? new BoardNavigationResponse.BoardNavigationItem(
                        next.getId(),
                        next.getTitle(),
                        next.getAuthorEmail(),
                        next.getCreatedAt().toString())
                        : null);
    }

    private YoutubeVideoMeta toVideoMeta(
            YoutubeVideoMeta currentMeta,
            Long boardId,
            YoutubeApiClient.FetchedYoutubeVideo fetchedVideo,
            String category,
            YoutubeImportStatus importStatus,
            LocalDateTime createdAt) {

        return YoutubeVideoMeta.builder()
                .id(currentMeta != null ? currentMeta.getId() : null)
                .boardId(boardId)
                .videoId(fetchedVideo.getVideoId())
                .channelId(fetchedVideo.getChannelId())
                .channelTitle(fetchedVideo.getChannelTitle())
                .sourceType(fetchedVideo.getSourceType())
                .sourceValue(fetchedVideo.getSourceValue())
                .category(normalizeText(category))
                .youtubeTitle(trimToLength(fetchedVideo.getYoutubeTitle(), 500))
                .youtubeDescription(fetchedVideo.getYoutubeDescription())
                .thumbnailUrl(fetchedVideo.getThumbnailUrl())
                .publishedAt(fetchedVideo.getPublishedAt())
                .duration(fetchedVideo.getDuration())
                .embedUrl(fetchedVideo.getEmbedUrl())
                .summary(currentMeta != null ? currentMeta.getSummary() : null)
                .editorComment(currentMeta != null ? currentMeta.getEditorComment() : null)
                .keyPoints(currentMeta != null ? currentMeta.getKeyPoints() : List.of())
                .aiGeneratedAt(currentMeta != null ? currentMeta.getAiGeneratedAt() : null)
                .importStatus(importStatus)
                .syncedAt(LocalDateTime.now())
                .createdAt(createdAt)
                .build();
    }

    private YoutubeVideoMeta rebuildMeta(YoutubeVideoMeta meta, Long boardId, YoutubeImportStatus status) {
        return YoutubeVideoMeta.builder()
                .id(meta.getId())
                .boardId(boardId)
                .videoId(meta.getVideoId())
                .channelId(meta.getChannelId())
                .channelTitle(meta.getChannelTitle())
                .sourceType(meta.getSourceType())
                .sourceValue(meta.getSourceValue())
                .category(meta.getCategory())
                .youtubeTitle(meta.getYoutubeTitle())
                .youtubeDescription(meta.getYoutubeDescription())
                .thumbnailUrl(meta.getThumbnailUrl())
                .publishedAt(meta.getPublishedAt())
                .duration(meta.getDuration())
                .embedUrl(meta.getEmbedUrl())
                .summary(meta.getSummary())
                .editorComment(meta.getEditorComment())
                .keyPoints(meta.getKeyPoints())
                .aiGeneratedAt(meta.getAiGeneratedAt())
                .importStatus(status)
                .syncedAt(LocalDateTime.now())
                .createdAt(meta.getCreatedAt())
                .updatedAt(meta.getUpdatedAt())
                .build();
    }

    private YoutubeVideoMeta enrichMeta(YoutubeVideoMeta meta, YoutubeGeneratedContent generatedContent) {
        return YoutubeVideoMeta.builder()
                .id(meta.getId())
                .boardId(meta.getBoardId())
                .videoId(meta.getVideoId())
                .channelId(meta.getChannelId())
                .channelTitle(meta.getChannelTitle())
                .sourceType(meta.getSourceType())
                .sourceValue(meta.getSourceValue())
                .category(meta.getCategory())
                .youtubeTitle(meta.getYoutubeTitle())
                .youtubeDescription(meta.getYoutubeDescription())
                .thumbnailUrl(meta.getThumbnailUrl())
                .publishedAt(meta.getPublishedAt())
                .duration(meta.getDuration())
                .embedUrl(meta.getEmbedUrl())
                .summary(trimNullableToLength(generatedContent.getSummary(), 1200))
                .editorComment(trimNullableToLength(generatedContent.getEditorComment(), 2000))
                .keyPoints(limitKeyPoints(generatedContent.getKeyPoints()))
                .aiGeneratedAt(LocalDateTime.now())
                .importStatus(meta.getImportStatus())
                .syncedAt(meta.getSyncedAt())
                .createdAt(meta.getCreatedAt())
                .updatedAt(meta.getUpdatedAt())
                .build();
    }

    private YoutubeImportSourceResponse toSourceResponse(YoutubeImportSource source) {
        return YoutubeImportSourceResponse.builder()
                .id(source.getId())
                .sourceType(source.getSourceType())
                .sourceValue(source.getSourceValue())
                .category(source.getCategory())
                .active(source.isActive())
                .autoPublish(source.isAutoPublish())
                .lastSyncedAt(source.getLastSyncedAt())
                .totalVideoCount(youtubeVideoMetaPersistencePort.countBySource(source.getSourceType(), source.getSourceValue()))
                .draftVideoCount(youtubeVideoMetaPersistencePort.countBySourceAndImportStatus(
                        source.getSourceType(),
                        source.getSourceValue(),
                        YoutubeImportStatus.DRAFT))
                .publishedVideoCount(youtubeVideoMetaPersistencePort.countBySourceAndImportStatus(
                        source.getSourceType(),
                        source.getSourceValue(),
                        YoutubeImportStatus.PUBLISHED))
                .hiddenVideoCount(youtubeVideoMetaPersistencePort.countBySourceAndImportStatus(
                        source.getSourceType(),
                        source.getSourceValue(),
                        YoutubeImportStatus.HIDDEN))
                .pendingAiCount(youtubeVideoMetaPersistencePort.countPendingAiBySource(
                        source.getSourceType(),
                        source.getSourceValue()))
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .build();
    }

    private Board rebuildBoard(
            Board board,
            String title,
            String content,
            List<String> hashtags,
            BoardStatus status,
            String authorEmail) {

        return Board.builder()
                .id(board.getId())
                .title(trimToLength(title, TITLE_MAX_LENGTH))
                .content(trimToLength(content, CONTENT_MAX_LENGTH))
                .authorEmail(authorEmail)
                .boardType(BoardType.MEDIA)
                .status(status)
                .viewCount(board.getViewCount())
                .likeCount(board.getLikeCount())
                .dislikeCount(board.getDislikeCount())
                .commentCount(board.getCommentCount())
                .reportCount(board.getReportCount())
                .hashtags(hashtags != null ? hashtags : List.of())
                .files(board.getFiles())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .replies(board.getReplies())
                .build();
    }

    private YoutubeImportStatus toImportStatus(BoardStatus status) {
        return switch (status) {
            case ACTIVE -> YoutubeImportStatus.PUBLISHED;
            case HIDDEN -> YoutubeImportStatus.HIDDEN;
            case DRAFT -> YoutubeImportStatus.DRAFT;
            default -> YoutubeImportStatus.FAILED;
        };
    }

    private Board loadBoard(Long boardId) {
        return boardPersistencePort.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException(boardId));
    }

    private YoutubeVideoMeta loadVideoMetaByBoardId(Long boardId) {
        return youtubeVideoMetaPersistencePort.findByBoardId(boardId)
                .orElseThrow(() -> new IllegalArgumentException("YouTube video meta not found: " + boardId));
    }

    private YoutubeImportStatus parseImportStatus(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        return YoutubeImportStatus.valueOf(normalized.toUpperCase());
    }

    private int safePage(int page) {
        return Math.max(page, 0);
    }

    private int safeSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private String defaultContent(String value) {
        String normalized = normalizeText(value);
        return normalized != null ? normalized : "";
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String normalized = normalizeText(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return "";
    }

    private String trimToLength(String value, int maxLength) {
        String normalized = value != null ? value : "";
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String trimNullableToLength(String value, int maxLength) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String toPreview(String content) {
        String normalized = defaultContent(content);
        if (normalized.length() <= PREVIEW_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, PREVIEW_MAX_LENGTH);
    }

    private List<String> normalizeHashtags(List<String> hashtags, String category) {
        Set<String> normalized = new LinkedHashSet<>();
        if (hashtags != null) {
            for (String hashtag : hashtags) {
                String value = normalizeText(hashtag);
                if (value != null) {
                    normalized.add(value.replace("#", "").replace(' ', '-'));
                }
            }
        }
        String normalizedCategory = normalizeText(category);
        if (normalizedCategory != null) {
            normalized.add(normalizedCategory.replace("#", "").replace(' ', '-'));
        }
        return new ArrayList<>(normalized);
    }

    private List<String> limitKeyPoints(List<String> keyPoints) {
        Set<String> normalized = new LinkedHashSet<>();
        if (keyPoints != null) {
            for (String keyPoint : keyPoints) {
                String value = trimNullableToLength(keyPoint, 300);
                if (value != null) {
                    normalized.add(value);
                }
                if (normalized.size() >= 3) {
                    break;
                }
            }
        }
        return new ArrayList<>(normalized);
    }

    private YoutubeSyncSummaryResponse emptySummary() {
        return YoutubeSyncSummaryResponse.builder()
                .sourceCount(0)
                .requestedCount(0)
                .fetchedCount(0)
                .importedCount(0)
                .updatedCount(0)
                .hiddenCount(0)
                .skippedCount(0)
                .failedCount(0)
                .build();
    }
}
