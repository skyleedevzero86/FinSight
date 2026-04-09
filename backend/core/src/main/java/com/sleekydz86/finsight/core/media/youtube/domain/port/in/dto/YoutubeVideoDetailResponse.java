package com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto;

import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardNavigationResponse;
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
public class YoutubeVideoDetailResponse {
    private Long boardId;
    private String title;
    private String content;
    private String authorEmail;
    private BoardType boardType;
    private BoardStatus boardStatus;
    private int viewCount;
    private int likeCount;
    private int dislikeCount;
    private int commentCount;
    private int reportCount;
    private List<String> hashtags;
    private String videoId;
    private String channelId;
    private String channelTitle;
    private YoutubeImportSourceType sourceType;
    private String sourceValue;
    private String category;
    private String youtubeTitle;
    private String youtubeDescription;
    private String thumbnailUrl;
    private String embedUrl;
    private LocalDateTime publishedAt;
    private String duration;
    private String summary;
    private String editorComment;
    private List<String> keyPoints;
    private LocalDateTime aiGeneratedAt;
    private YoutubeImportStatus importStatus;
    private LocalDateTime syncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BoardNavigationResponse navigation;
}
