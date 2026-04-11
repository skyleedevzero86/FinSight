package com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto;

import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSourceType;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeVideoListResponse {
    private Long boardId;
    private String title;
    private String previewContent;
    private String authorEmail;
    private BoardStatus boardStatus;
    private String videoId;
    private String channelId;
    private String channelTitle;
    private YoutubeImportSourceType sourceType;
    private String sourceValue;
    private String category;
    private String thumbnailUrl;
    private LocalDateTime publishedAt;
    private String duration;
    private String summary;
    private YoutubeImportStatus importStatus;
    private List<String> hashtags;
    private LocalDateTime aiGeneratedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
