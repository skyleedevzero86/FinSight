package com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto;

import java.util.List;

public record LiveVodFeedResponse(
        String title,
        String tab,
        String featuredVideoId,
        String featuredTitle,
        String featuredThumbnailUrl,
        List<LiveVodSectionResponse> sections
) {
    public LiveVodFeedResponse {
        sections = sections == null ? List.of() : List.copyOf(sections);
    }

    public record LiveVodSectionResponse(
            String heading,
            List<LiveVodItemResponse> items
    ) {
        public LiveVodSectionResponse {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record LiveVodItemResponse(
            String videoId,
            String title,
            String thumbnailUrl,
            String watchUrl,
            String embedUrl,
            String channelTitle,
            long favoriteCount,
            long ratingCount,
            double avgRating
    ) {
        public LiveVodItemResponse {
            if (favoriteCount < 0) favoriteCount = 0;
            if (ratingCount < 0) ratingCount = 0;
            if (avgRating < 0) avgRating = 0;
        }

        public LiveVodItemResponse(
                String videoId,
                String title,
                String thumbnailUrl,
                String watchUrl,
                String embedUrl,
                String channelTitle) {
            this(videoId, title, thumbnailUrl, watchUrl, embedUrl, channelTitle, 0, 0, 0.0);
        }
    }
}
