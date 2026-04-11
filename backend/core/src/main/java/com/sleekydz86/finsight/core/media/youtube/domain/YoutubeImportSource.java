package com.sleekydz86.finsight.core.media.youtube.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeImportSource {
    private Long id;
    private YoutubeImportSourceType sourceType;
    private String sourceValue;
    private String category;
    private boolean active;
    private boolean autoPublish;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
