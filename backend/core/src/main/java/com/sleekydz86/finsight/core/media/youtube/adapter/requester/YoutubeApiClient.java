package com.sleekydz86.finsight.core.media.youtube.adapter.requester;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sleekydz86.finsight.core.media.youtube.adapter.requester.properties.YoutubeApiProperties;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSource;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSourceType;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class YoutubeApiClient {

    private static final Logger log = LoggerFactory.getLogger(YoutubeApiClient.class);
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(
            "(?:youtu\\.be/|youtube\\.com/(?:watch\\?v=|shorts/|embed/|live/))([A-Za-z0-9_-]{11})");
    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "^PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?$");

    private final RestTemplate restTemplate;
    private final YoutubeApiProperties youtubeApiProperties;

    public YoutubeApiClient(RestTemplate restTemplate, YoutubeApiProperties youtubeApiProperties) {
        this.restTemplate = restTemplate;
        this.youtubeApiProperties = youtubeApiProperties;
    }

    public List<FetchedYoutubeVideo> fetchBySource(YoutubeImportSource source) {
        if (!isConfigured()) {
            log.warn("YouTube API 키가 설정되어 있지 않습니다");
            return List.of();
        }

        return switch (source.getSourceType()) {
            case CHANNEL_HANDLE -> fetchByChannelHandle(source.getSourceValue(), source.getCategory());
            case CHANNEL_ID -> fetchByChannelId(source.getSourceValue(), source.getCategory());
            case PLAYLIST_ID -> fetchByPlaylistId(
                    source.getSourceValue(),
                    source.getCategory(),
                    source.getSourceType(),
                    source.getSourceValue(),
                    resolveMaxResults());
            case MANUAL_URL -> fetchVideosByUrls(
                    List.of(source.getSourceValue()),
                    source.getCategory(),
                    source.getSourceType(),
                    source.getSourceValue());
        };
    }

    public List<FetchedYoutubeVideo> fetchVideosByUrls(
            List<String> urls,
            String category,
            YoutubeImportSourceType sourceType,
            String sourceValue) {

        if (!isConfigured()) {
            log.warn("YouTube API 키가 설정되어 있지 않습니다");
            return List.of();
        }

        Set<String> videoIds = new LinkedHashSet<>();
        for (String url : urls) {
            extractVideoId(url).ifPresent(videoIds::add);
        }

        return fetchVideoDetails(new ArrayList<>(videoIds), category, sourceType, sourceValue);
    }

    public List<FetchedYoutubeVideo> fetchVideosByUrls(List<String> urls, String category) {
        return fetchVideosByUrls(urls, category, YoutubeImportSourceType.MANUAL_URL, "manual");
    }

    public List<FetchedYoutubeVideo> fetchLiveVodFeed(String category, String searchQuery) {
        if (!isConfigured()) {
            log.warn("YouTube API 키가 설정되어 있지 않습니다");
            return List.of();
        }

        String channelId = youtubeApiProperties.getLiveChannelId();
        if (channelId != null && !channelId.isBlank()) {
            return fetchByChannelId(channelId.trim(), category);
        }

        String handle = youtubeApiProperties.getLiveChannelHandle();
        if (handle != null && !handle.isBlank()) {
            return fetchByChannelHandle(handle.trim(), category);
        }

        String query = (searchQuery != null && !searchQuery.isBlank())
                ? searchQuery.trim()
                : youtubeApiProperties.getLiveSearchQuery();
        return searchVideos(query, category);
    }

    public Optional<FetchedYoutubeVideo> fetchFeaturedLiveVideo(String category) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        String configured = youtubeApiProperties.getFeaturedLiveVideoId();
        if (configured == null || configured.isBlank()) {
            return Optional.empty();
        }

        Optional<String> videoId = extractVideoId(configured.trim());
        if (videoId.isEmpty()) {
            return Optional.empty();
        }

        List<FetchedYoutubeVideo> videos = fetchVideoDetails(
                List.of(videoId.get()),
                category,
                YoutubeImportSourceType.MANUAL_URL,
                "featured-live:" + videoId.get());
        if (videos.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(videos.get(0));
    }

    public List<FetchedYoutubeVideo> fetchMoreSectionVideos(String category) {
        return fetchMoreSectionVideos(category, null);
    }

    public List<FetchedYoutubeVideo> fetchChannelUploads(
            String handle,
            String category,
            int maxResults,
            int minDurationSeconds) {
        if (!isConfigured() || handle == null || handle.isBlank()) {
            return List.of();
        }

        int limit = maxResults > 0 ? Math.min(maxResults, 50) : resolveMaxResults();
        ChannelListResponse response = get(
                "/channels",
                UriComponentsBuilder.newInstance()
                        .queryParam("part", "contentDetails,snippet")
                        .queryParam("forHandle", normalizeHandle(handle))
                        .queryParam("key", youtubeApiProperties.getApiKey())
                        .build()
                        .toUriString(),
                ChannelListResponse.class);

        ChannelItem item = firstChannel(response);
        if (item == null
                || item.contentDetails() == null
                || item.contentDetails().relatedPlaylists() == null
                || item.contentDetails().relatedPlaylists().uploads() == null) {
            return List.of();
        }

        List<FetchedYoutubeVideo> uploads = fetchByPlaylistId(
                item.contentDetails().relatedPlaylists().uploads(),
                category,
                YoutubeImportSourceType.CHANNEL_HANDLE,
                handle,
                Math.min(Math.max(limit * 2, limit), 50));
        return filterByMinDuration(uploads, minDurationSeconds).stream()
                .limit(limit)
                .toList();
    }

    public List<FetchedYoutubeVideo> fetchMoreSectionVideos(
            String category,
            YoutubeApiProperties.MoreChannelSource channel) {
        if (!isConfigured()) {
            return List.of();
        }

        YoutubeApiProperties.MoreChannelSource source = channel;
        if (source == null) {
            List<YoutubeApiProperties.MoreChannelSource> channels =
                    youtubeApiProperties.resolveMoreChannels();
            if (channels.isEmpty()) {
                return List.of();
            }
            source = channels.get(0);
        }

        String handle = source.getHandle();
        if (handle == null || handle.isBlank()) {
            return List.of();
        }

        int maxResults = source.getMaxResults() > 0
                ? source.getMaxResults()
                : youtubeApiProperties.getMoreMaxResults();
        if (maxResults <= 0) {
            maxResults = 4;
        }
        maxResults = Math.min(maxResults, 50);

        int minDurationSeconds = source.getMinDurationSeconds() > 0
                ? source.getMinDurationSeconds()
                : youtubeApiProperties.getMoreMinDurationSeconds();

        return fetchChannelUploads(handle, category, maxResults, minDurationSeconds);
    }

    private List<FetchedYoutubeVideo> searchChannelVideos(
            String channelId,
            String query,
            String category,
            String sourceValue,
            int maxResults) {
        if (channelId == null || channelId.isBlank()) {
            return List.of();
        }

        String q = (query == null || query.isBlank()) ? "경제" : query.trim();
        SearchListResponse response = get(
                "/search",
                UriComponentsBuilder.newInstance()
                        .queryParam("part", "snippet")
                        .queryParam("channelId", channelId)
                        .queryParam("type", "video")
                        .queryParam("q", q)
                        .queryParam("order", "date")
                        .queryParam("maxResults", Math.max(1, Math.min(maxResults, 50)))
                        .queryParam("key", youtubeApiProperties.getApiKey())
                        .build()
                        .toUriString(),
                SearchListResponse.class);

        if (response == null || response.items() == null || response.items().isEmpty()) {
            return List.of();
        }

        List<String> videoIds = response.items().stream()
                .map(SearchItem::id)
                .filter(Objects::nonNull)
                .map(SearchId::videoId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return fetchVideoDetails(
                videoIds,
                category,
                YoutubeImportSourceType.CHANNEL_HANDLE,
                sourceValue);
    }

    private List<FetchedYoutubeVideo> filterByMinDuration(
            List<FetchedYoutubeVideo> videos,
            int minSeconds) {
        if (videos == null || videos.isEmpty()) {
            return List.of();
        }
        if (minSeconds <= 0) {
            return videos;
        }
        List<FetchedYoutubeVideo> filtered = videos.stream()
                .filter(v -> durationSeconds(v.duration()) >= minSeconds)
                .toList();
        return filtered.isEmpty() ? videos : filtered;
    }

    private List<FetchedYoutubeVideo> mergeUniqueVideos(
            List<FetchedYoutubeVideo> primary,
            List<FetchedYoutubeVideo> secondary) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<FetchedYoutubeVideo> merged = new ArrayList<>();
        for (FetchedYoutubeVideo video : primary) {
            if (video == null || video.videoId() == null || !seen.add(video.videoId())) {
                continue;
            }
            merged.add(video);
        }
        for (FetchedYoutubeVideo video : secondary) {
            if (video == null || video.videoId() == null || !seen.add(video.videoId())) {
                continue;
            }
            merged.add(video);
        }
        return merged;
    }

    private int durationSeconds(String iso8601Duration) {
        if (iso8601Duration == null || iso8601Duration.isBlank()) {
            return 0;
        }
        Matcher matcher = DURATION_PATTERN.matcher(iso8601Duration);
        if (!matcher.matches()) {
            return 0;
        }
        int hours = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : 0;
        int minutes = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
        int seconds = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        return hours * 3600 + minutes * 60 + seconds;
    }

    public List<FetchedYoutubeVideo> searchVideos(String query, String category) {
        if (!isConfigured() || query == null || query.isBlank()) {
            return List.of();
        }

        int maxResults = youtubeApiProperties.getLiveMaxResults();
        if (maxResults <= 0) {
            maxResults = 24;
        }
        maxResults = Math.min(maxResults, 50);

        SearchListResponse response = get(
                "/search",
                UriComponentsBuilder.newInstance()
                        .queryParam("part", "snippet")
                        .queryParam("type", "video")
                        .queryParam("q", query)
                        .queryParam("order", "date")
                        .queryParam("maxResults", maxResults)
                        .queryParam("key", youtubeApiProperties.getApiKey())
                        .build()
                        .toUriString(),
                SearchListResponse.class);

        if (response == null || response.items() == null || response.items().isEmpty()) {
            return List.of();
        }

        List<String> videoIds = response.items().stream()
                .map(SearchItem::id)
                .filter(Objects::nonNull)
                .map(SearchId::videoId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return fetchVideoDetails(videoIds, category, YoutubeImportSourceType.MANUAL_URL, "live-search:" + query);
    }

    public Optional<String> extractVideoId(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }

        String trimmed = rawValue.trim();
        if (trimmed.matches("[A-Za-z0-9_-]{11}")) {
            return Optional.of(trimmed);
        }

        Matcher matcher = VIDEO_ID_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(1));
        }

        return Optional.empty();
    }

    private List<FetchedYoutubeVideo> fetchByChannelHandle(String handle, String category) {
        ChannelListResponse response = get(
                "/channels",
                UriComponentsBuilder.newInstance()
                        .queryParam("part", "contentDetails,snippet")
                        .queryParam("forHandle", normalizeHandle(handle))
                        .queryParam("key", youtubeApiProperties.getApiKey())
                        .build()
                        .toUriString(),
                ChannelListResponse.class);

        ChannelItem item = firstChannel(response);
        if (item == null || item.contentDetails() == null || item.contentDetails().relatedPlaylists() == null) {
            return List.of();
        }

        return fetchByPlaylistId(
                item.contentDetails().relatedPlaylists().uploads(),
                category,
                YoutubeImportSourceType.CHANNEL_HANDLE,
                handle,
                resolveMaxResults());
    }

    private List<FetchedYoutubeVideo> fetchByChannelId(String channelId, String category) {
        ChannelListResponse response = get(
                "/channels",
                UriComponentsBuilder.newInstance()
                        .queryParam("part", "contentDetails,snippet")
                        .queryParam("id", channelId)
                        .queryParam("key", youtubeApiProperties.getApiKey())
                        .build()
                        .toUriString(),
                ChannelListResponse.class);

        ChannelItem item = firstChannel(response);
        if (item == null || item.contentDetails() == null || item.contentDetails().relatedPlaylists() == null) {
            return List.of();
        }

        return fetchByPlaylistId(
                item.contentDetails().relatedPlaylists().uploads(),
                category,
                YoutubeImportSourceType.CHANNEL_ID,
                channelId,
                resolveMaxResults());
    }

    private List<FetchedYoutubeVideo> fetchByPlaylistId(
            String playlistId,
            String category,
            YoutubeImportSourceType sourceType,
            String sourceValue,
            int maxResults) {

        PlaylistItemListResponse response = get(
                "/playlistItems",
                UriComponentsBuilder.newInstance()
                        .queryParam("part", "snippet,contentDetails")
                        .queryParam("playlistId", playlistId)
                        .queryParam("maxResults", Math.max(1, Math.min(maxResults, 50)))
                        .queryParam("key", youtubeApiProperties.getApiKey())
                        .build()
                        .toUriString(),
                PlaylistItemListResponse.class);

        if (response == null || response.items() == null || response.items().isEmpty()) {
            return List.of();
        }

        List<String> videoIds = response.items().stream()
                .map(PlaylistItem::contentDetails)
                .filter(Objects::nonNull)
                .map(PlaylistContentDetails::videoId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return fetchVideoDetails(videoIds, category, sourceType, sourceValue);
    }

    private List<FetchedYoutubeVideo> fetchVideoDetails(
            List<String> videoIds,
            String category,
            YoutubeImportSourceType sourceType,
            String sourceValue) {

        if (videoIds == null || videoIds.isEmpty()) {
            return List.of();
        }

        VideoListResponse response = get(
                "/videos",
                UriComponentsBuilder.newInstance()
                        .queryParam("part", "snippet,contentDetails,status")
                        .queryParam("id", String.join(",", videoIds))
                        .queryParam("key", youtubeApiProperties.getApiKey())
                        .build()
                        .toUriString(),
                VideoListResponse.class);

        if (response == null || response.items() == null) {
            return List.of();
        }

        List<FetchedYoutubeVideo> videos = new ArrayList<>();
        for (VideoItem item : response.items()) {
            if (item == null || item.status() == null || item.snippet() == null) {
                continue;
            }
            if (!Boolean.TRUE.equals(item.status().embeddable())) {
                continue;
            }
            if (item.status().privacyStatus() != null && !"public".equalsIgnoreCase(item.status().privacyStatus())) {
                continue;
            }

            videos.add(FetchedYoutubeVideo.builder()
                    .videoId(item.id())
                    .channelId(item.snippet().channelId())
                    .channelTitle(item.snippet().channelTitle())
                    .sourceType(sourceType)
                    .sourceValue(sourceValue)
                    .category(category)
                    .youtubeTitle(item.snippet().title())
                    .youtubeDescription(item.snippet().description())
                    .thumbnailUrl(resolveThumbnailUrl(item.snippet().thumbnails()))
                    .publishedAt(parseDateTime(item.snippet().publishedAt()))
                    .duration(item.contentDetails() != null ? item.contentDetails().duration() : null)
                    .embedUrl(buildEmbedUrl(item.id()))
                    .build());
        }

        return videos;
    }

    private <T> T get(String path, String query, Class<T> responseType) {
        String url = youtubeApiProperties.getBaseUrl() + path + query;
        try {
            return restTemplate.getForObject(url, responseType);
        } catch (Exception e) {
            log.error("YouTube API 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    private ChannelItem firstChannel(ChannelListResponse response) {
        if (response == null || response.items() == null || response.items().isEmpty()) {
            return null;
        }
        return response.items().get(0);
    }

    private boolean isConfigured() {
        return youtubeApiProperties.getApiKey() != null && !youtubeApiProperties.getApiKey().isBlank();
    }

    private String normalizeHandle(String handle) {
        if (handle == null) {
            return "";
        }
        String trimmed = handle.trim();
        return trimmed.startsWith("@") ? trimmed.substring(1) : trimmed;
    }

    private int resolveMaxResults() {
        int configured = youtubeApiProperties.getMaxResultsPerSource();
        if (configured <= 0) {
            return 25;
        }
        return Math.min(configured, 50);
    }

    private String buildEmbedUrl(String videoId) {
        return "https://www.youtube.com/embed/" + videoId;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value).toLocalDateTime();
    }

    private String resolveThumbnailUrl(VideoThumbnails thumbnails) {
        if (thumbnails == null) {
            return null;
        }
        if (thumbnails.maxres() != null && thumbnails.maxres().url() != null) {
            return thumbnails.maxres().url();
        }
        if (thumbnails.high() != null && thumbnails.high().url() != null) {
            return thumbnails.high().url();
        }
        if (thumbnails.medium() != null && thumbnails.medium().url() != null) {
            return thumbnails.medium().url();
        }
        if (thumbnails.defaultThumbnail() != null) {
            return thumbnails.defaultThumbnail().url();
        }
        return null;
    }

    @Builder
    public record FetchedYoutubeVideo(
            String videoId,
            String channelId,
            String channelTitle,
            YoutubeImportSourceType sourceType,
            String sourceValue,
            String category,
            String youtubeTitle,
            String youtubeDescription,
            String thumbnailUrl,
            LocalDateTime publishedAt,
            String duration,
            String embedUrl
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchListResponse(
            @JsonProperty("items") List<SearchItem> items
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchItem(
            @JsonProperty("id") SearchId id
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchId(
            @JsonProperty("videoId") String videoId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChannelListResponse(
            @JsonProperty("items") List<ChannelItem> items
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChannelItem(
            @JsonProperty("id") String id,
            @JsonProperty("contentDetails") ChannelContentDetails contentDetails
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChannelContentDetails(
            @JsonProperty("relatedPlaylists") RelatedPlaylists relatedPlaylists
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RelatedPlaylists(
            @JsonProperty("uploads") String uploads
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlaylistItemListResponse(
            @JsonProperty("items") List<PlaylistItem> items
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlaylistItem(
            @JsonProperty("contentDetails") PlaylistContentDetails contentDetails
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlaylistContentDetails(
            @JsonProperty("videoId") String videoId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VideoListResponse(
            @JsonProperty("items") List<VideoItem> items
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VideoItem(
            @JsonProperty("id") String id,
            @JsonProperty("snippet") VideoSnippet snippet,
            @JsonProperty("contentDetails") VideoContentDetails contentDetails,
            @JsonProperty("status") VideoStatus status
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VideoSnippet(
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("channelId") String channelId,
            @JsonProperty("channelTitle") String channelTitle,
            @JsonProperty("publishedAt") String publishedAt,
            @JsonProperty("thumbnails") VideoThumbnails thumbnails
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VideoContentDetails(
            @JsonProperty("duration") String duration
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VideoStatus(
            @JsonProperty("privacyStatus") String privacyStatus,
            @JsonProperty("embeddable") Boolean embeddable
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VideoThumbnails(
            @JsonProperty("default") Thumbnail defaultThumbnail,
            @JsonProperty("medium") Thumbnail medium,
            @JsonProperty("high") Thumbnail high,
            @JsonProperty("maxres") Thumbnail maxres
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Thumbnail(
            @JsonProperty("url") String url
    ) {
    }
}
