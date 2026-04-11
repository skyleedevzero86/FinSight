package com.sleekydz86.finsight.core.media.youtube.domain.port.in;

import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeAiEnrichmentSummaryResponse;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeSyncSummaryResponse;

public interface YoutubeMediaImportUseCase {
    YoutubeSyncSummaryResponse syncActiveSources();

    YoutubeAiEnrichmentSummaryResponse enrichPendingDraftVideos();
}
