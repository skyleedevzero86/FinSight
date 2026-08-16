package com.sleekydz86.finsight.core.media.youtube.adapter.requester;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sleekydz86.finsight.core.media.youtube.adapter.requester.properties.YoutubeApiProperties;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSource;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSourceType;
import lombok.Builder;
import lombok.Getter;
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
                    source.getSourceValue());
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
        if (item == null || item.contentDetails == null || item.contentDetails.relatedPlaylists == null) {
            return List.of();
        }

        return fetchByPlaylistId(
                item.contentDetails.relatedPlaylists.uploads,
                category,
                YoutubeImportSourceType.CHANNEL_HANDLE,
                handle);
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
        if (item == null || item.contentDetails == null || item.contentDetails.relatedPlaylists == null) {
            return List.of();
        }

        return fetchByPlaylistId(
                item.contentDetails.relatedPlaylists.uploads,
                category,
                YoutubeImportSourceType.CHANNEL_ID,
                channelId);
    }

    private List<FetchedYoutubeVideo> fetchByPlaylistId(
            String playlistId,
            String category,
            YoutubeImportSourceType sourceType,
            String sourceValue) {

        PlaylistItemListResponse response = get(
                "/playlistItems",
                UriComponentsBuilder.newInstance()
                        .queryParam("part", "snippet,contentDetails")
                        .queryParam("playlistId", playlistId)
                        .queryParam("maxResults", resolveMaxResults())
                        .queryParam("key", youtubeApiProperties.getApiKey())
                        .build()
                        .toUriString(),
                PlaylistItemListResponse.class);

        if (response == null || response.items == null || response.items.isEmpty()) {
            return List.of();
        }

        List<String> videoIds = response.items.stream()
                .map(item -> item.contentDetails)
                .filter(Objects::nonNull)
                .map(contentDetails -> contentDetails.videoId)
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

        if (response == null || response.items == null) {
            return List.of();
        }

        List<FetchedYoutubeVideo> videos = new ArrayList<>();
        for (VideoItem item : response.items) {
            if (item == null || item.status == null || item.snippet == null) {
                continue;
            }
            if (!Boolean.TRUE.equals(item.status.embeddable)) {
                continue;
            }
            if (item.status.privacyStatus != null && !"public".equalsIgnoreCase(item.status.privacyStatus)) {
                continue;
            }

            videos.add(FetchedYoutubeVideo.builder()
                    .videoId(item.id)
                    .channelId(item.snippet.channelId)
                    .channelTitle(item.snippet.channelTitle)
                    .sourceType(sourceType)
                    .sourceValue(sourceValue)
                    .category(category)
                    .youtubeTitle(item.snippet.title)
                    .youtubeDescription(item.snippet.description)
                    .thumbnailUrl(resolveThumbnailUrl(item.snippet.thumbnails))
                    .publishedAt(parseDateTime(item.snippet.publishedAt))
                    .duration(item.contentDetails != null ? item.contentDetails.duration : null)
                    .embedUrl(buildEmbedUrl(item.id))
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
        if (response == null || response.items == null || response.items.isEmpty()) {
            return null;
        }
        return response.items.get(0);
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
        if (thumbnails.maxres != null && thumbnails.maxres.url != null) {
            return thumbnails.maxres.url;
        }
        if (thumbnails.high != null && thumbnails.high.url != null) {
            return thumbnails.high.url;
        }
        if (thumbnails.medium != null && thumbnails.medium.url != null) {
            return thumbnails.medium.url;
        }
        if (thumbnails.defaultThumbnail != null) {
            return thumbnails.defaultThumbnail.url;
        }
        return null;
    }

    @Getter
    @Builder
    public static class FetchedYoutubeVideo {
        private String videoId;
        private String channelId;
        private String channelTitle;
        private YoutubeImportSourceType sourceType;
        private String sourceValue;
        private String category;
        private String youtubeTitle;
        private String youtubeDescription;
        private String thumbnailUrl;
        private LocalDateTime publishedAt;
        private String duration;
        private String embedUrl;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChannelListResponse {
        @JsonProperty("items")
        public List<ChannelItem> items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChannelItem {
        @JsonProperty("contentDetails")
        public ChannelContentDetails contentDetails;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChannelContentDetails {
        @JsonProperty("relatedPlaylists")
        public RelatedPlaylists relatedPlaylists;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RelatedPlaylists {
        @JsonProperty("uploads")
        public String uploads;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlaylistItemListResponse {
        @JsonProperty("items")
        public List<PlaylistItem> items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlaylistItem {
        @JsonProperty("contentDetails")
        public PlaylistContentDetails contentDetails;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlaylistContentDetails {
        @JsonProperty("videoId")
        public String videoId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoListResponse {
        @JsonProperty("items")
        public List<VideoItem> items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoItem {
        @JsonProperty("id")
        public String id;

        @JsonProperty("snippet")
        public VideoSnippet snippet;

        @JsonProperty("contentDetails")
        public VideoContentDetails contentDetails;

        @JsonProperty("status")
        public VideoStatus status;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoSnippet {
        @JsonProperty("title")
        public String title;

        @JsonProperty("description")
        public String description;

        @JsonProperty("channelId")
        public String channelId;

        @JsonProperty("channelTitle")
        public String channelTitle;

        @JsonProperty("publishedAt")
        public String publishedAt;

        @JsonProperty("thumbnails")
        public VideoThumbnails thumbnails;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoContentDetails {
        @JsonProperty("duration")
        public String duration;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoStatus {
        @JsonProperty("privacyStatus")
        public String privacyStatus;

        @JsonProperty("embeddable")
        public Boolean embeddable;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoThumbnails {
        @JsonProperty("default")
        public Thumbnail defaultThumbnail;

        @JsonProperty("medium")
        public Thumbnail medium;

        @JsonProperty("high")
        public Thumbnail high;

        @JsonProperty("maxres")
        public Thumbnail maxres;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Thumbnail {
        @JsonProperty("url")
        public String url;
    }
}
