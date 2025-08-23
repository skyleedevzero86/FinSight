package com.sleekydz86.finsight.batch.news.scrap;

import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@EnableScheduling
@Component
public class NewsScrapScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsScrapScheduler.class);
    private static final long JOB_INTERVAL_MINUTE = 15;
    private static final long EVERY_JOB_INTERVAL_MINUTE = JOB_INTERVAL_MINUTE * 60 * 1000;
    private static final long SCRAP_LIMIT_SIZE = 5L;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JobLauncher jobLauncher;
    private final Job newsScrapJob;

    public NewsScrapScheduler(JobLauncher jobLauncher,
                              @Qualifier("newsScrapJob") Job newsScrapJob) {
        this.jobLauncher = jobLauncher;
        this.newsScrapJob = newsScrapJob;
    }

    @Scheduled(fixedRate = EVERY_JOB_INTERVAL_MINUTE)
    @Timed("news.scrap.scheduler.duration")
    @Retry(name = "newsScrapSchedulerRetry")
    public void runNewsScrapJob() throws Exception {
        String publishTimeAfter = createSeoulLocationTime()
                .minusMinutes(JOB_INTERVAL_MINUTE)
                .format(DATE_TIME_FORMATTER);

        var jobParameters = new JobParametersBuilder()
                .addString("publishTimeAfter", publishTimeAfter)
                .addLong("limit", SCRAP_LIMIT_SIZE)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        CompletableFuture.runAsync(() -> {
            try {
                jobLauncher.run(newsScrapJob, jobParameters);
                log.info("뉴스 스크랩 작업 완료: {}", publishTimeAfter);
            } catch (Exception e) {
                log.error("뉴스 스크랩 작업 실패: {}", e.getMessage(), e);
            }
        });
    }

    private LocalDateTime createSeoulLocationTime() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
