package com.sleekydz86.finsight.batch.news.scrap;

import com.sleekydz86.finsight.core.news.crawl.NewsCrawlLatency;
import com.sleekydz86.finsight.core.news.domain.port.in.NewsCommandUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
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
    private final AtomicInteger skippedJobCount = new AtomicInteger(0);

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
        log.info("정기 뉴스 스크래핑 작업 시작");
        scheduledJobCount.incrementAndGet();

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("scheduledTime", LocalDateTime.now().toString())
                    .toJobParameters();

            if (finish(jobLauncher.run(newsScrapJob, jobParameters))) {
                log.info("정기 뉴스 스크래핑 작업이 성공적으로 완료되었습니다");
            }

        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("뉴스 스크래핑 작업이 이미 실행 중입니다");
        } catch (JobRestartException e) {
            log.error("뉴스 스크래핑 작업 재시작 실패", e);
            failedJobCount.incrementAndGet();
        } catch (JobInstanceAlreadyCompleteException e) {
            log.warn("뉴스 스크래핑 작업 인스턴스가 이미 완료되었습니다");
        } catch (JobParametersInvalidException e) {
            log.error("뉴스 스크래핑 작업 파라미터가 올바르지 않습니다", e);
            failedJobCount.incrementAndGet();
        } catch (Exception e) {
            log.error("뉴스 스크래핑 작업 실행 중 예기치 않은 오류 발생", e);
            failedJobCount.incrementAndGet();
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void scheduleFullNewsScraping() {
        log.info("전체 뉴스 스크래핑 작업 시작");
        scheduledJobCount.incrementAndGet();

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("scheduledTime", LocalDateTime.now().toString())
                    .addString("jobType", "FULL_SCRAPING")
                    .toJobParameters();

            if (finish(jobLauncher.run(newsScrapJob, jobParameters))) {
                log.info("전체 뉴스 스크래핑 작업이 성공적으로 완료되었습니다");
            }

        } catch (Exception e) {
            log.error("전체 뉴스 스크래핑 작업 실행 실패", e);
            failedJobCount.incrementAndGet();
        }
    }

    public CompletableFuture<Void> executeManualNewsScraping() {
        log.info("수동 뉴스 스크래핑 실행");
        scheduledJobCount.incrementAndGet();

        return CompletableFuture.runAsync(() -> {
            try {
                JobParameters jobParameters = new JobParametersBuilder()
                        .addLong("timestamp", System.currentTimeMillis())
                        .addString("scheduledTime", LocalDateTime.now().toString())
                        .addString("jobType", "MANUAL_SCRAPING")
                        .toJobParameters();

                if (finish(jobLauncher.run(newsScrapJob, jobParameters))) {
                    log.info("수동 뉴스 스크래핑이 성공적으로 완료되었습니다");
                }

            } catch (Exception e) {
                log.error("수동 뉴스 스크래핑 실행 실패", e);
                failedJobCount.incrementAndGet();
                throw new RuntimeException("수동 뉴스 스크래핑 실행 실패", e);
            }
        });
    }

    public SchedulingMetrics getSchedulingMetrics() {
        return new SchedulingMetrics(
                scheduledJobCount.get(),
                successfulJobCount.get(),
                failedJobCount.get(),
                skippedJobCount.get()
        );
    }

    public void resetMetrics() {
        scheduledJobCount.set(0);
        successfulJobCount.set(0);
        failedJobCount.set(0);
        skippedJobCount.set(0);
        log.info("스케줄링 지표 초기화 완료");
    }

    private boolean finish(JobExecution execution) {
        int ran = execution.getExecutionContext().getInt("newsCrawlRan", -1);
        int skips = execution.getExecutionContext().getInt("newsCrawlLockSkips", 0);
        if (NewsCrawlLatency.lockSkipOnly(ran, skips)) {
            skippedJobCount.incrementAndGet();
            log.warn("수집 잠금 충돌로 이번 실행을 성공으로 기록하지 않습니다");
            return false;
        }
        successfulJobCount.incrementAndGet();
        return true;
    }

    public record SchedulingMetrics(
            int scheduledJobCount,
            int successfulJobCount,
            int failedJobCount,
            int skippedJobCount
    ) {
        public double getSuccessRate() {
            if (scheduledJobCount == 0) return 0.0;
            return (double) successfulJobCount / scheduledJobCount * 100;
        }
    }
}