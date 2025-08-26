package com.sleekydz86.finsight.batch.news.scrap;

import com.sleekydz86.finsight.core.news.domain.port.in.NewsCommandUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class NewsScrapScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsScrapScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job newsScrapJob;
    private final NewsCommandUseCase newsCommandUseCase;

    private final AtomicInteger scheduledJobCount = new AtomicInteger(0);
    private final AtomicInteger successfulJobCount = new AtomicInteger(0);
    private final AtomicInteger failedJobCount = new AtomicInteger(0);

    public NewsScrapScheduler(
            JobLauncher jobLauncher,
            @Qualifier("newsScrapJob") Job newsScrapJob,
            NewsCommandUseCase newsCommandUseCase) {
        this.jobLauncher = jobLauncher;
        this.newsScrapJob = newsScrapJob;
        this.newsCommandUseCase = newsCommandUseCase;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void scheduleNewsScraping() {
        log.info("Starting scheduled news scraping job");
        scheduledJobCount.incrementAndGet();

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("scheduledTime", LocalDateTime.now().toString())
                    .toJobParameters();

            jobLauncher.run(newsScrapJob, jobParameters);
            successfulJobCount.incrementAndGet();
            log.info("Scheduled news scraping job completed successfully");

        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("News scraping job is already running");
        } catch (JobRestartException e) {
            log.error("Failed to restart news scraping job", e);
            failedJobCount.incrementAndGet();
        } catch (JobInstanceAlreadyCompleteException e) {
            log.warn("News scraping job instance already completed");
        } catch (JobParametersInvalidException e) {
            log.error("Invalid job parameters for news scraping job", e);
            failedJobCount.incrementAndGet();
        } catch (Exception e) {
            log.error("Unexpected error during news scraping job execution", e);
            failedJobCount.incrementAndGet();
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduleFullNewsScraping() {
        log.info("Starting full news scraping job");
        scheduledJobCount.incrementAndGet();

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("scheduledTime", LocalDateTime.now().toString())
                    .addString("jobType", "FULL_SCRAPING")
                    .toJobParameters();

            jobLauncher.run(newsScrapJob, jobParameters);
            successfulJobCount.incrementAndGet();
            log.info("Full news scraping job completed successfully");

        } catch (Exception e) {
            log.error("Failed to execute full news scraping job", e);
            failedJobCount.incrementAndGet();
        }
    }

    public CompletableFuture<Void> executeManualNewsScraping() {
        log.info("Executing manual news scraping");
        scheduledJobCount.incrementAndGet();

        return CompletableFuture.runAsync(() -> {
            try {
                JobParameters jobParameters = new JobParametersBuilder()
                        .addLong("timestamp", System.currentTimeMillis())
                        .addString("scheduledTime", LocalDateTime.now().toString())
                        .addString("jobType", "MANUAL_SCRAPING")
                        .toJobParameters();

                jobLauncher.run(newsScrapJob, jobParameters);
                successfulJobCount.incrementAndGet();
                log.info("Manual news scraping completed successfully");

            } catch (Exception e) {
                log.error("Failed to execute manual news scraping", e);
                failedJobCount.incrementAndGet();
                throw new RuntimeException("Manual news scraping failed", e);
            }
        });
    }

    public SchedulingMetrics getSchedulingMetrics() {
        return new SchedulingMetrics(
                scheduledJobCount.get(),
                successfulJobCount.get(),
                failedJobCount.get()
        );
    }

    public void resetMetrics() {
        scheduledJobCount.set(0);
        successfulJobCount.set(0);
        failedJobCount.set(0);
        log.info("Scheduling metrics reset");
    }

    public record SchedulingMetrics(
            int scheduledJobCount,
            int successfulJobCount,
            int failedJobCount
    ) {
        public double getSuccessRate() {
            if (scheduledJobCount == 0) return 0.0;
            return (double) successfulJobCount / scheduledJobCount * 100;
        }
    }
}