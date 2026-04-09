package com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeSyncSummaryResponse {
    private int sourceCount;
    private int requestedCount;
    private int fetchedCount;
    private int importedCount;
    private int updatedCount;
    private int hiddenCount;
    private int skippedCount;
    private int failedCount;

    public YoutubeSyncSummaryResponse merge(YoutubeSyncSummaryResponse other) {
        if (other == null) {
            return this;
        }

        return YoutubeSyncSummaryResponse.builder()
                .sourceCount(this.sourceCount + other.sourceCount)
                .requestedCount(this.requestedCount + other.requestedCount)
                .fetchedCount(this.fetchedCount + other.fetchedCount)
                .importedCount(this.importedCount + other.importedCount)
                .updatedCount(this.updatedCount + other.updatedCount)
                .hiddenCount(this.hiddenCount + other.hiddenCount)
                .skippedCount(this.skippedCount + other.skippedCount)
                .failedCount(this.failedCount + other.failedCount)
                .build();
    }
}
