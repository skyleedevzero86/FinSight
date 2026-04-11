package com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto;

import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeSourceReviewResponse {
    private Long sourceId;
    private YoutubeImportSourceType sourceType;
    private String sourceValue;
    private String category;
    private boolean active;
    private boolean autoPublish;
    private LocalDateTime lastSyncedAt;
    private long totalVideoCount;
    private long draftVideoCount;
    private long publishedVideoCount;
    private long hiddenVideoCount;
    private long pendingAiCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private PaginationResponse<YoutubeVideoListResponse> videos;
}
