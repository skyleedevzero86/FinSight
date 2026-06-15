package com.sleekydz86.finsight.batch.youtube;

import com.sleekydz86.finsight.core.media.youtube.domain.port.in.YoutubeMediaImportUseCase;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeAiEnrichmentSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class YoutubeAiEnrichmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(YoutubeAiEnrichmentScheduler.class);

    private final YoutubeMediaImportUseCase youtubeMediaImportUseCase;

    @Value("${youtube.ai.enabled:true}")
    private boolean enabled;

    public YoutubeAiEnrichmentScheduler(YoutubeMediaImportUseCase youtubeMediaImportUseCase) {
        this.youtubeMediaImportUseCase = youtubeMediaImportUseCase;
    }

    @Scheduled(fixedDelayString = "${youtube.ai.schedule.fixed-delay-ms:300000}")
    public void enrichYoutubeDraftVideos() {
        if (!enabled) {
            return;
        }

        YoutubeAiEnrichmentSummaryResponse summary = youtubeMediaImportUseCase.enrichPendingDraftVideos();
        log.info("YouTube AI 보강 완료 - 요청: {}, 보강: {}, 건너뜀: {}, 실패: {}",
                summary.getRequestedCount(),
                summary.getEnrichedCount(),
                summary.getSkippedCount(),
                summary.getFailedCount());
    }
}
