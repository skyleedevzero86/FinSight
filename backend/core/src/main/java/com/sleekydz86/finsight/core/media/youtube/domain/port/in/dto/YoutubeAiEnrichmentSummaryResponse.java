package com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YoutubeAiEnrichmentSummaryResponse {
    private int requestedCount;
    private int enrichedCount;
    private int skippedCount;
    private int failedCount;

    public YoutubeAiEnrichmentSummaryResponse merge(YoutubeAiEnrichmentSummaryResponse other) {
        if (other == null) {
            return this;
        }

        return YoutubeAiEnrichmentSummaryResponse.builder()
                .requestedCount(this.requestedCount + other.requestedCount)
                .enrichedCount(this.enrichedCount + other.enrichedCount)
                .skippedCount(this.skippedCount + other.skippedCount)
                .failedCount(this.failedCount + other.failedCount)
                .build();
    }
}
