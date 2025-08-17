package com.sleekydz86.finsight.batch.news.scrap;

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

@EnableScheduling
@Component
public class NewsScrapScheduler {

    private static final long JOB_INTERVAL_MINUTE = 15;
    private static final long EVERY_JOB_INTERVAL_MINUTE = JOB_INTERVAL_MINUTE * 60 * 1000;
    private static final long SCRAP_LIMIT_SIZE = 5L;
    private static final long SCRAP_LIMIT_SIZE_VALUE = 3;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JobLauncher jobLauncher;
    private final Job newsScrapJob;

    public NewsScrapScheduler(JobLauncher jobLauncher,
                              @Qualifier("newsScrapJob") Job newsScrapJob) {
        this.jobLauncher = jobLauncher;
        this.newsScrapJob = newsScrapJob;
    }

    @Scheduled(fixedRate = EVERY_JOB_INTERVAL_MINUTE)
    public void runNewsScrapJob() throws Exception {
        String publishTimeAfter = createSeoulLocationTime()
                .minusMinutes(JOB_INTERVAL_MINUTE)
                .format(DATE_TIME_FORMATTER);

        var jobParameters = new JobParametersBuilder()
                .addString("publishTimeAfter", publishTimeAfter)
                .addString("publishTimeAfter2", createSeoulLocationTime().toString())
                .addLong("limit", SCRAP_LIMIT_SIZE)
                .toJobParameters();

        jobLauncher.run(newsScrapJob, jobParameters);
    }

    private LocalDateTime createSeoulLocationTime() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}