package com.sleekydz86.finsight.batch.youtube;

import com.sleekydz86.finsight.core.media.youtube.domain.port.in.YoutubeMediaImportUseCase;
import com.sleekydz86.finsight.core.media.youtube.domain.port.in.dto.YoutubeSyncSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class YoutubeImportScheduler {

    private static final Logger log = LoggerFactory.getLogger(YoutubeImportScheduler.class);

    private final YoutubeMediaImportUseCase youtubeMediaImportUseCase;

    @Value("${youtube.import.enabled:true}")
    private boolean enabled;

    public YoutubeImportScheduler(YoutubeMediaImportUseCase youtubeMediaImportUseCase) {
        this.youtubeMediaImportUseCase = youtubeMediaImportUseCase;
    }

    @Scheduled(fixedDelayString = "${youtube.import.schedule.fixed-delay-ms:3600000}")
    public void syncYoutubeSources() {
        if (!enabled) {
            return;
        }

        YoutubeSyncSummaryResponse summary = youtubeMediaImportUseCase.syncActiveSources();
        log.info("YouTube 동기화 완료 - 소스: {}, 신규 가져옴: {}, 갱신: {}, 실패: {}",
                summary.getSourceCount(),
                summary.getImportedCount(),
                summary.getUpdatedCount(),
                summary.getFailedCount());
    }
}
