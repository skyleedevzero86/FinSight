package com.sleekydz86.finsight.batch.news.scrap.job;

import com.sleekydz86.finsight.batch.news.scrap.tasklet.NewsCrawlingTasklet;
import com.sleekydz86.finsight.batch.news.scrap.tasklet.OpenAiAnalysisTasklet;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class NewsScrapJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final NewsCrawlingTasklet newsCrawlingTasklet;
    private final OpenAiAnalysisTasklet openAiAnalysisTasklet;

    public NewsScrapJobConfig(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              DataSource dataSource,
                              NewsCrawlingTasklet newsCrawlingTasklet,
                              OpenAiAnalysisTasklet openAiAnalysisTasklet) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.dataSource = dataSource;
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
                .<NewsJpaEntity, NewsJpaEntity>chunk(100, transactionManager)
                .reader(aiAnalysisReader())
                .processor(aiAnalysisProcessor())
                .writer(aiAnalysisWriter())
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<NewsJpaEntity> aiAnalysisReader() {
        return new JdbcCursorItemReaderBuilder<NewsJpaEntity>()
                .name("aiAnalysisReader")
                .dataSource(dataSource)
                .sql("SELECT * FROM news WHERE ai_overview IS NULL")
                .rowMapper(new DataClassRowMapper<>(NewsJpaEntity.class))
                .fetchSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<NewsJpaEntity, NewsJpaEntity> aiAnalysisProcessor() {
        return news -> {
            // AI 분석 처리 로직
            return news;
        };
    }

    @Bean
    public ItemWriter<NewsJpaEntity> aiAnalysisWriter() {
        return new JdbcBatchItemWriterBuilder<NewsJpaEntity>()
                .dataSource(dataSource)
                .sql("UPDATE news SET ai_overview = :overview, ai_sentiment_type = :sentimentType WHERE id = :id")
                .beanMapped()
                .build();
    }
}
