package com.sleekydz86.finsight.batch.news.scrap.job;

import com.sleekydz86.finsight.batch.news.scrap.tasklet.NewsCrawlingTasklet;
import com.sleekydz86.finsight.batch.news.scrap.tasklet.OpenAiAnalysisTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class NewsScrapJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final NewsCrawlingTasklet newsCrawlingTasklet;
    private final OpenAiAnalysisTasklet openAiAnalysisTasklet;

    public NewsScrapJobConfig(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              NewsCrawlingTasklet newsCrawlingTasklet,
                              OpenAiAnalysisTasklet openAiAnalysisTasklet) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.newsCrawlingTasklet = newsCrawlingTasklet;
        this.openAiAnalysisTasklet = openAiAnalysisTasklet;
    }

    @Bean
    public Job newsScrapJob() {
        return new JobBuilder("newsScrapJob", jobRepository)
                .start(newsCrawlingStep())
                .next(aiAnalysisStep())
                .build();
    }

    @Bean
    public Step newsCrawlingStep() {
        return new StepBuilder("newsCrawlingStep", jobRepository)
                .tasklet(newsCrawlingTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step aiAnalysisStep() {
        return new StepBuilder("aiAnalysisStep", jobRepository)
                .tasklet(openAiAnalysisTasklet, transactionManager)
                .build();
    }
}