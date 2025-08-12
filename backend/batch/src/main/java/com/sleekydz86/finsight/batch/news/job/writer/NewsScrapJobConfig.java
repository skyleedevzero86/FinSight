package com.sleekydz86.finsight.batch.news.job.writer;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableScheduling
public class NewsScrapJobConfig {

    private static final long FIFTY_MINUTE = 15 * 60 * 1000;
    private static final int MAXIMUM_CRAWLING_DATA_CHUNK_SIZE = 30;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobLauncher jobLauncher;

    public NewsScrapJobConfig(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              JobLauncher jobLauncher) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.jobLauncher = jobLauncher;
    }

    @Scheduled(fixedRate = FIFTY_MINUTE)
    public void runNewsJob() throws Exception {
        var jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(newsScrapJob(), jobParameters);
    }

    @Bean
    public Job newsScrapJob() {
        return new JobBuilder("newsScrapJob", jobRepository)
                .start(newsCollectionStep())
                .next(aiAnalysisStep())
                .next(dataSaveStep())
                .build();
    }


    @Bean
    public Step newsCollectionStep() {
        return new StepBuilder("newsCollectionStep", jobRepository)
                .<Void, String>chunk(MAXIMUM_CRAWLING_DATA_CHUNK_SIZE, transactionManager)
                .reader(null)
                .processor(null)
                .writer(null)
                .build();
    }


    @Bean
    public Step aiAnalysisStep() {
        return new StepBuilder("aiAnalysisStep", jobRepository)
                .<String, String>chunk(MAXIMUM_CRAWLING_DATA_CHUNK_SIZE, transactionManager)
                .reader(null)
                .processor(null)
                .writer(null)
                .build();
    }

    @Bean
    public Step dataSaveStep() {
        return new StepBuilder("dataSaveStep", jobRepository)
                .<String, String>chunk(MAXIMUM_CRAWLING_DATA_CHUNK_SIZE, transactionManager)
                .reader(null)
                .writer(null)
                .build();
    }
}