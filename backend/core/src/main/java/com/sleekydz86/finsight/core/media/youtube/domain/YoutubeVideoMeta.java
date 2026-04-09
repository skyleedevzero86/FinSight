package com.sleekydz86.finsight.core.media.youtube.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeVideoMeta {
    private Long id;
    private Long boardId;
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
    private String summary;
    private String editorComment;
    @Builder.Default
    private List<String> keyPoints = new ArrayList<>();
    private LocalDateTime aiGeneratedAt;
    private YoutubeImportStatus importStatus;
    private LocalDateTime syncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
