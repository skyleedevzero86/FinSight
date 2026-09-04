package com.sleekydz86.finsight.batch.board.schedul;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.board.moderation.enabled", havingValue = "true", matchIfMissing = true)
public class BoardModerationScheduler {

    private static final Logger log = LoggerFactory.getLogger(BoardModerationScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job boardModerationJob;

    @Value("${app.board.moderation.report-threshold:5}")
    private int reportThreshold;

    public BoardModerationScheduler(
            JobLauncher jobLauncher,
            @Qualifier("boardModerationJob") Job boardModerationJob) {
        this.jobLauncher = jobLauncher;
        this.boardModerationJob = boardModerationJob;
    }

    @Scheduled(cron = "${app.board.moderation.cron:0 0 3 * * *}")
    public void scheduleHideOverReported() {
        log.info("게시판 신고 과다 숨김 배치 시작 - threshold={}", reportThreshold);
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addLong("reportThreshold", (long) reportThreshold)
                    .toJobParameters();
            jobLauncher.run(boardModerationJob, params);
            log.info("게시판 신고 과다 숨김 배치 요청 완료");
        } catch (Exception e) {
            log.error("게시판 신고 과다 숨김 배치 실행 실패", e);
        }
    }
}
