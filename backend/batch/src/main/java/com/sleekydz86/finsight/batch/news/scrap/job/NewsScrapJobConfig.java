package com.sleekydz86.finsight.batch.news.scrap.job;

import com.sleekydz86.finsight.batch.news.scrap.tasklet.NewsCrawlingTasklet;
import com.sleekydz86.finsight.batch.news.scrap.tasklet.OpenAiAnalysisTasklet;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sql.DataSource;

@Configuration
public class NewsScrapJobConfig {

    private static final Logger log = LoggerFactory.getLogger(NewsScrapJobConfig.class);
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
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                        log.info("뉴스 스크래핑 배치 작업 시작: {}", jobExecution.getJobInstance().getJobName());
                    }

                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        log.info("뉴스 스크래핑 배치 작업 완료: 상태={}, 종료시간={}",
                                jobExecution.getStatus(), jobExecution.getEndTime());
                    }
                })
                .build();
    }

    @Bean
    public Step newsCrawlingStep() {
        return new StepBuilder("newsCrawlingStep", jobRepository)
                .tasklet(newsCrawlingTasklet, transactionManager)
                .listener(new StepExecutionListener() {
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        log.info("뉴스 크롤링 단계 시작");
                    }

                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        log.info("뉴스 크롤링 단계 완료: 처리된 항목={}", stepExecution.getReadCount());
                        return stepExecution.getExitStatus();
                    }
                })
                .build();
    }

    @Bean
    public Step aiAnalysisStep() {
        return new StepBuilder("aiAnalysisStep", jobRepository)
                .<NewsJpaEntity, NewsJpaEntity>chunk(50, transactionManager)
                .reader(aiAnalysisReader())
                .processor(aiAnalysisProcessor())
                .writer(aiAnalysisWriter())
                .faultTolerant()
                .retry(Exception.class)
                .retryLimit(3)
                .skip(Exception.class)
                .skipLimit(10)
                .listener(new ChunkListener() {
                    @Override
                    public void beforeChunk(ChunkContext context) {
                        log.debug("AI 분석 청크 처리 시작");
                    }

                    @Override
                    public void afterChunk(ChunkContext context) {
                        log.debug("AI 분석 청크 처리 완료");
                    }

                    @Override
                    public void afterChunkError(ChunkContext context) {
                        log.error("AI 분석 청크 처리 오류");
                    }
                })
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<NewsJpaEntity> aiAnalysisReader() {
        JpaPagingItemReader<NewsJpaEntity> reader = new JpaPagingItemReader<>();

        if (transactionManager instanceof JpaTransactionManager) {
            JpaTransactionManager jpaTransactionManager = (JpaTransactionManager) transactionManager;
            reader.setEntityManagerFactory(jpaTransactionManager.getEntityManagerFactory());
        } else {
            throw new IllegalStateException("JpaTransactionManager가 필요합니다");
        }

        reader.setQueryString("SELECT n FROM NewsJpaEntity n WHERE n.overview IS NULL ORDER BY n.createdAt");
        reader.setPageSize(50);
        reader.setSaveState(false);
        return reader;
    }

    @Bean
    public ItemProcessor<NewsJpaEntity, NewsJpaEntity> aiAnalysisProcessor() {
        return newsJpaEntity -> {
            try {
                // AI 분석 처리 로직
                log.debug("AI 분석 처리 중: 뉴스 ID={}", newsJpaEntity.getId());
                return newsJpaEntity;
            } catch (Exception e) {
                log.error("AI 분석 처리 오류: 뉴스 ID={}, 오류={}", newsJpaEntity.getId(), e.getMessage());
                throw e;
            }
        };
    }

    @Bean
    public ItemWriter<NewsJpaEntity> aiAnalysisWriter() {
        return items -> {
            try {
                // 배치 저장 로직
                log.debug("AI 분석 결과 저장 중: {}개 항목", items.size());
                // 실제 저장 로직 구현
            } catch (Exception e) {
                log.error("AI 분석 결과 저장 오류: {}", e.getMessage());
                throw e;
            }
        };
    }

    @Bean
    public JobParametersIncrementer jobParametersIncrementer() {
        return new RunIdIncrementer();
    }
}