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
import com.sleekydz86.finsight.core.media.youtube.adapter.requester.properties.YoutubeApiProperties;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeGeneratedContent;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSource;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSourceType;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportStatus;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeVideoMeta;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.YoutubeMediaAdminUseCase;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.YoutubeMediaImportUseCase;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.YoutubeMediaQueryUseCase;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.LiveVodFeedResponse;
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
    private final YoutubeApiProperties youtubeApiProperties;

    @Value("${youtube.ai.batch-size:10}")
    private int aiBatchSize;

    public YoutubeMediaService(
            YoutubeVideoMetaPersistencePort youtubeVideoMetaPersistencePort,
            YoutubeImportSourcePersistencePort youtubeImportSourcePersistencePort,
            BoardPersistencePort boardPersistencePort,
            YoutubeApiClient youtubeApiClient,
            YoutubeAiContentRequester youtubeAiContentRequester,
            YoutubeApiProperties youtubeApiProperties) {
        this.youtubeVideoMetaPersistencePort = youtubeVideoMetaPersistencePort;
        this.youtubeImportSourcePersistencePort = youtubeImportSourcePersistencePort;
        this.boardPersistencePort = boardPersistencePort;
        this.youtubeApiClient = youtubeApiClient;
        this.youtubeAiContentRequester = youtubeAiContentRequester;
        this.youtubeApiProperties = youtubeApiProperties;
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
    public LiveVodFeedResponse getLiveVodFeed(String tab) {
        String normalizedTab = normalizeLiveTab(tab);
        try {
            return buildLiveVodFeedForTab(normalizedTab);
        } catch (Exception e) {
            log.error("LIVE/VOD 피드 조회 실패 tab={}", normalizedTab, e);
            return new LiveVodFeedResponse(
                    resolveFeedTitle(normalizedTab),
                    normalizedTab,
                    null,
                    null,
                    null,
                    List.of());
        }
    }

    private LiveVodFeedResponse buildLiveVodFeedForTab(String normalizedTab) {
        String category = "ALL".equals(normalizedTab) ? null : normalizedTab;

        if ("ALL".equals(normalizedTab)) {
            return buildAllTabFeed(category);
        }
        if ("LIVE".equals(normalizedTab)) {
            return buildLiveTabFeed(category);
        }

        Optional<YoutubeApiProperties.MoreChannelSource> channelTab =
                resolveChannelTabSource(normalizedTab);
        if (channelTab.isPresent()) {
            return buildChannelOnlyFeed(normalizedTab, category, channelTab.get());
        }

        return buildTopicOnlyFeed(normalizedTab, category);
    }

    private LiveVodFeedResponse buildAllTabFeed(String category) {
        Optional<YoutubeApiClient.FetchedYoutubeVideo> featuredLive = fetchFeaturedLiveSafe(category);
        List<YoutubeApiClient.FetchedYoutubeVideo> fetched = fetchLiveSearchSafe(
                "ALL", resolveLiveSearchQuery("ALL"));

        if (featuredLive.isPresent()) {
            YoutubeApiClient.FetchedYoutubeVideo featured = featuredLive.get();
            List<LiveVodFeedResponse.LiveVodItemResponse> items = fetched.stream()
                    .filter(v -> v.videoId() != null && !v.videoId().equals(featured.videoId()))
                    .map(this::toLiveItem)
                    .toList();
            return buildLiveVodFeed(
                    "ALL",
                    featured.videoId(),
                    featured.youtubeTitle(),
                    featured.thumbnailUrl(),
                    items,
                    category,
                    true);
        }

        return toLiveVodFeedFromFetched("ALL", fetched, category, true);
    }

    private LiveVodFeedResponse buildLiveTabFeed(String category) {
        Optional<YoutubeApiClient.FetchedYoutubeVideo> featuredLive = fetchFeaturedLiveSafe(category);
        List<YoutubeApiClient.FetchedYoutubeVideo> fetched = fetchLiveSearchSafe(
                "LIVE", resolveLiveSearchQuery("LIVE"));

        if (featuredLive.isPresent()) {
            YoutubeApiClient.FetchedYoutubeVideo featured = featuredLive.get();
            List<LiveVodFeedResponse.LiveVodItemResponse> items = fetched.stream()
                    .filter(v -> v.videoId() != null && !v.videoId().equals(featured.videoId()))
                    .map(this::toLiveItem)
                    .toList();
            return buildLiveVodFeed(
                    "LIVE",
                    featured.videoId(),
                    featured.youtubeTitle(),
                    featured.thumbnailUrl(),
                    items,
                    category,
                    false);
        }

        return toLiveVodFeedFromFetched("LIVE", fetched, category, false);
    }

    private LiveVodFeedResponse buildChannelOnlyFeed(
            String tab,
            String category,
            YoutubeApiProperties.MoreChannelSource source) {
        YoutubeApiProperties.MoreChannelSource enriched = copyChannelSource(source);
        enriched.setMaxResults(Math.max(16, source.getMaxResults()));
        List<YoutubeApiClient.FetchedYoutubeVideo> channelVideos = List.of();
        try {
            channelVideos = youtubeApiClient.fetchMoreSectionVideos(category, enriched);
        } catch (Exception e) {
            log.warn("채널 탭 영상 조회 실패 handle={}: {}", source.getHandle(), e.getMessage());
        }
        return toLiveVodFeedFromFetched(tab, channelVideos, category, false);
    }

    private LiveVodFeedResponse buildTopicOnlyFeed(String tab, String category) {
        List<YoutubeApiClient.FetchedYoutubeVideo> fetched = fetchLiveSearchSafe(
                tab, resolveLiveSearchQuery(tab));
        return toLiveVodFeedFromFetched(tab, fetched, category, false);
    }

    private Optional<YoutubeApiClient.FetchedYoutubeVideo> fetchFeaturedLiveSafe(String category) {
        try {
            return youtubeApiClient.fetchFeaturedLiveVideo(category);
        } catch (Exception e) {
            log.warn("featured live 조회 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private List<YoutubeApiClient.FetchedYoutubeVideo> fetchLiveSearchSafe(String tab, String query) {
        try {
            return youtubeApiClient.fetchLiveVodFeed(tab, query);
        } catch (Exception e) {
            log.warn("LIVE 검색 실패 tab={}: {}", tab, e.getMessage());
            return List.of();
        }
    }

    private String normalizeLiveTab(String tab) {
        if (tab == null || tab.isBlank()) {
            return "ALL";
        }
        return tab.trim().toUpperCase();
    }

    private Optional<YoutubeApiProperties.MoreChannelSource> resolveChannelTabSource(String tab) {
        String expectedHandle = switch (tab) {
            case "GOMHEE" -> "gomhee";
            case "SYUKA" -> "syukaworld";
            case "BOOTYFUL" -> "money-multiple";
            default -> null;
        };
        if (expectedHandle == null) {
            return Optional.empty();
        }
        return youtubeApiProperties.resolveMoreChannels().stream()
                .filter(channel -> channel.getHandle() != null)
                .filter(channel -> expectedHandle.equalsIgnoreCase(channel.getHandle().trim()))
                .findFirst();
    }

    private YoutubeApiProperties.MoreChannelSource copyChannelSource(
            YoutubeApiProperties.MoreChannelSource source) {
        YoutubeApiProperties.MoreChannelSource copy = new YoutubeApiProperties.MoreChannelSource();
        copy.setHandle(source.getHandle());
        copy.setHeading(source.getHeading());
        copy.setSearchQuery(source.getSearchQuery());
        copy.setMaxResults(source.getMaxResults());
        copy.setMinDurationSeconds(source.getMinDurationSeconds());
        copy.setTabs(source.getTabs() == null ? List.of() : new ArrayList<>(source.getTabs()));
        return copy;
    }

    private String resolveLiveSearchQuery(String tab) {
        return switch (tab) {
            case "LIVE" -> "서울경제TV 라이브 증시";
            case "MARKET" -> "미국 증시 시장 브리핑";
            case "GOMHEE" -> "박곰희 경제 증시";
            case "SYUKA" -> "슈카월드 경제 시사";
            case "BOOTYFUL" -> "부티플 투자 주식";
            case "THEME" -> "주식 테마 분석 AI 반도체";
            case "MACRO" -> "글로벌 매크로 경제 환율";
            case "ALL" -> null;
            default -> null;
        };
    }

    private LiveVodFeedResponse toLiveVodFeedFromPublished(
            String tab, List<YoutubeVideoListResponse> videos, String category) {
        YoutubeVideoListResponse featured = videos.get(0);
        List<LiveVodFeedResponse.LiveVodItemResponse> items = videos.stream()
                .skip(1)
                .map(this::toLiveItem)
                .toList();

        return buildLiveVodFeed(
                tab,
                featured.getVideoId(),
                featured.getTitle(),
                featured.getThumbnailUrl(),
                items,
                category);
    }

    private LiveVodFeedResponse toLiveVodFeedFromFetched(
            String tab,
            List<YoutubeApiClient.FetchedYoutubeVideo> videos,
            String category) {
        return toLiveVodFeedFromFetched(tab, videos, category, true);
    }

    private LiveVodFeedResponse toLiveVodFeedFromFetched(
            String tab,
            List<YoutubeApiClient.FetchedYoutubeVideo> videos,
            String category,
            boolean includeMoreChannels) {
        if (videos == null || videos.isEmpty()) {
            return buildLiveVodFeed(tab, null, null, null, List.of(), category, includeMoreChannels);
        }

        YoutubeApiClient.FetchedYoutubeVideo featured = videos.get(0);
        List<LiveVodFeedResponse.LiveVodItemResponse> items = videos.stream()
                .skip(1)
                .map(this::toLiveItem)
                .toList();

        return buildLiveVodFeed(
                tab,
                featured.videoId(),
                featured.youtubeTitle(),
                featured.thumbnailUrl(),
                items,
                category,
                includeMoreChannels);
    }

    private LiveVodFeedResponse buildLiveVodFeed(
            String tab,
            String featuredVideoId,
            String featuredTitle,
            String featuredThumbnailUrl,
            List<LiveVodFeedResponse.LiveVodItemResponse> mainItems,
            String category) {
        return buildLiveVodFeed(
                tab, featuredVideoId, featuredTitle, featuredThumbnailUrl, mainItems, category, true);
    }

    private LiveVodFeedResponse buildLiveVodFeed(
            String tab,
            String featuredVideoId,
            String featuredTitle,
            String featuredThumbnailUrl,
            List<LiveVodFeedResponse.LiveVodItemResponse> mainItems,
            String category,
            boolean includeMoreChannels) {
        List<LiveVodFeedResponse.LiveVodSectionResponse> sections = chunkMainLiveItems(mainItems, tab);
        if (includeMoreChannels) {
            appendMoreSection(sections, category, featuredVideoId, tab);
        }
        return new LiveVodFeedResponse(
                resolveFeedTitle(tab),
                tab,
                featuredVideoId,
                featuredTitle,
                featuredThumbnailUrl,
                sections);
    }

    private String resolveFeedTitle(String tab) {
        return switch (tab) {
            case "LIVE" -> "finsight LIVE";
            case "MARKET" -> "시장 브리핑";
            case "GOMHEE" -> "박곰희 TV";
            case "SYUKA" -> "슈카월드";
            case "BOOTYFUL" -> "부티플";
            case "THEME" -> "테마 분석";
            case "MACRO" -> "글로벌 매크로";
            default -> "finsight LIVE";
        };
    }

    private LiveVodFeedResponse.LiveVodItemResponse toLiveItem(YoutubeVideoListResponse v) {
        return new LiveVodFeedResponse.LiveVodItemResponse(
                v.getVideoId(),
                v.getTitle(),
                v.getThumbnailUrl(),
                "https://www.youtube.com/watch?v=" + v.getVideoId(),
                "https://www.youtube.com/embed/" + v.getVideoId(),
                v.getChannelTitle());
    }

    private LiveVodFeedResponse.LiveVodItemResponse toLiveItem(YoutubeApiClient.FetchedYoutubeVideo v) {
        return new LiveVodFeedResponse.LiveVodItemResponse(
                v.videoId(),
                v.youtubeTitle(),
                v.thumbnailUrl(),
                "https://www.youtube.com/watch?v=" + v.videoId(),
                v.embedUrl(),
                v.channelTitle());
    }

    private List<LiveVodFeedResponse.LiveVodSectionResponse> chunkMainLiveItems(
            List<LiveVodFeedResponse.LiveVodItemResponse> items,
            String tab) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        if ("ALL".equals(tab)) {
            List<LiveVodFeedResponse.LiveVodSectionResponse> sections = new ArrayList<>();
            int firstSize = Math.min(4, items.size());
            sections.add(new LiveVodFeedResponse.LiveVodSectionResponse(
                    "최신 VOD",
                    new ArrayList<>(items.subList(0, firstSize))));
            if (items.size() > firstSize) {
                sections.add(new LiveVodFeedResponse.LiveVodSectionResponse(
                        "관련 영상",
                        new ArrayList<>(items.subList(firstSize, items.size()))));
            }
            return sections;
        }

        return List.of(new LiveVodFeedResponse.LiveVodSectionResponse(
                "",
                new ArrayList<>(items)));
    }

    private void appendMoreSection(
            List<LiveVodFeedResponse.LiveVodSectionResponse> sections,
            String category,
            String featuredVideoId,
            String tab) {
        Set<String> existingIds = sections.stream()
                .flatMap(section -> section.items().stream())
                .map(LiveVodFeedResponse.LiveVodItemResponse::videoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (featuredVideoId != null && !featuredVideoId.isBlank()) {
            existingIds.add(featuredVideoId);
        }

        for (YoutubeApiProperties.MoreChannelSource channel
                : youtubeApiProperties.resolveMoreChannels()) {
            if (!channel.matchesTab(tab)) {
                continue;
            }

            List<YoutubeApiClient.FetchedYoutubeVideo> moreVideos;
            try {
                moreVideos = youtubeApiClient.fetchMoreSectionVideos(category, channel);
            } catch (Exception e) {
                log.warn("추가 채널 섹션 조회 실패 handle={}: {}", channel.getHandle(), e.getMessage());
                continue;
            }
            if (moreVideos == null || moreVideos.isEmpty()) {
                continue;
            }

            int limit = channel.getMaxResults() > 0
                    ? channel.getMaxResults()
                    : Math.max(1, youtubeApiProperties.getMoreMaxResults());

            List<LiveVodFeedResponse.LiveVodItemResponse> moreItems = moreVideos.stream()
                    .filter(v -> v.videoId() != null && !existingIds.contains(v.videoId()))
                    .map(this::toLiveItem)
                    .limit(limit)
                    .toList();
            if (moreItems.isEmpty()) {
                continue;
            }

            moreItems.stream()
                    .map(LiveVodFeedResponse.LiveVodItemResponse::videoId)
                    .filter(Objects::nonNull)
                    .forEach(existingIds::add);

            String heading = channel.getHeading();
            if (heading == null || heading.isBlank()) {
                heading = channel.getHandle();
            }
            sections.add(new LiveVodFeedResponse.LiveVodSectionResponse(heading, moreItems));
        }
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
                Optional<YoutubeVideoMeta> existingMeta = youtubeVideoMetaPersistencePort.findByVideoId(fetchedVideo.videoId());
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
                            .title(trimToLength(fetchedVideo.youtubeTitle(), TITLE_MAX_LENGTH))
                            .content(trimToLength(defaultContent(fetchedVideo.youtubeDescription()), CONTENT_MAX_LENGTH))
                            .authorEmail(authorEmail)
                            .boardType(BoardType.MEDIA)
                            .status(BoardStatus.DRAFT)
                            .hashtags(hashtags)
                            .build());

                    youtubeVideoMetaPersistencePort.save(toVideoMeta(
                            null,
                            savedBoard.getId(),
                            fetchedVideo,
                            normalizeText(fetchedVideo.category()),
                            YoutubeImportStatus.DRAFT,
                            null));

                    summary = summary.merge(YoutubeSyncSummaryResponse.builder().importedCount(1).build());
                }
            } catch (Exception e) {
                log.error("Failed to import YouTube video {}", fetchedVideo.videoId(), e);
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
                .videoId(fetchedVideo.videoId())
                .channelId(fetchedVideo.channelId())
                .channelTitle(fetchedVideo.channelTitle())
                .sourceType(fetchedVideo.sourceType())
                .sourceValue(fetchedVideo.sourceValue())
                .category(normalizeText(category))
                .youtubeTitle(trimToLength(fetchedVideo.youtubeTitle(), 500))
                .youtubeDescription(fetchedVideo.youtubeDescription())
                .thumbnailUrl(fetchedVideo.thumbnailUrl())
                .publishedAt(fetchedVideo.publishedAt())
                .duration(fetchedVideo.duration())
                .embedUrl(fetchedVideo.embedUrl())
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
