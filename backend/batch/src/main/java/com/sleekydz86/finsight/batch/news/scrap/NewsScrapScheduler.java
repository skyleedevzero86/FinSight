package com.sleekydz86.finsight.batch.news.scrap;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@EnableScheduling
@Component
public class NewsScrapScheduler {

    private static final long EVERY_FIFTY_MINUTE = 15 * 60 * 1000;
    private static final long SCRAP_LIMIT_SIZE = 5L;

    private final JobLauncher jobLauncher;
    private final Job newsScrapJob;

    public NewsScrapScheduler(JobLauncher jobLauncher,
                              @Qualifier("newsScrapJob") Job newsScrapJob) {
        this.jobLauncher = jobLauncher;
        this.newsScrapJob = newsScrapJob;
    }

    @Scheduled(fixedRate = EVERY_FIFTY_MINUTE)
    public void runNewsScrapJob() throws Exception {
        var jobParameters = new JobParametersBuilder()
                .addString("publishTimeAfter", LocalDateTime.now().minusHours(1).toString())
                .addLong("limit", SCRAP_LIMIT_SIZE)
                .toJobParameters();

        jobLauncher.run(newsScrapJob, jobParameters);
    }
}
