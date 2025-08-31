package com.sleekydz86.finsight.batch.news.scrap.job;

import com.sleekydz86.finsight.batch.news.scrap.tasklet.DjlAnalysisTasklet;
import com.sleekydz86.finsight.batch.news.scrap.tasklet.NewsCrawlingTasklet;
import com.sleekydz86.finsight.batch.news.scrap.tasklet.OpenAiAnalysisTasklet;
import com.sleekydz86.finsight.batch.news.scrap.tasklet.SentimentAnalysisTasklet;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import com.sleekydz86.finsight.core.news.domain.port.out.DjlSentimentAnalysisPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class NewsScrapJobConfig {

    private static final Logger log = LoggerFactory.getLogger(NewsScrapJobConfig.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final NewsCrawlingTasklet newsCrawlingTasklet;
    private final OpenAiAnalysisTasklet openAiAnalysisTasklet;
    private final DjlAnalysisTasklet djlAnalysisTasklet;
    private final SentimentAnalysisTasklet sentimentAnalysisTasklet;

    private final ConcurrentHashMap<String, AtomicLong> stepExecutionMetrics = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> errorMetrics = new ConcurrentHashMap<>();

    public NewsScrapJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            EntityManagerFactory entityManagerFactory,
            NewsCrawlingTasklet newsCrawlingTasklet,
            OpenAiAnalysisTasklet openAiAnalysisTasklet,
            DjlAnalysisTasklet djlAnalysisTasklet,
            SentimentAnalysisTasklet sentimentAnalysisTasklet) {

        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.entityManagerFactory = entityManagerFactory;
        this.newsCrawlingTasklet = newsCrawlingTasklet;
        this.openAiAnalysisTasklet = openAiAnalysisTasklet;
        this.djlAnalysisTasklet = djlAnalysisTasklet;
        this.sentimentAnalysisTasklet = sentimentAnalysisTasklet;
    }

    @Bean
    public Job newsScrapJob() {
        return new JobBuilder("newsScrapJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                        log.info("Starting news scraping job: {}", jobExecution.getJobInstance().getJobName());
                        stepExecutionMetrics.computeIfAbsent("jobs_started", k -> new AtomicLong(0)).incrementAndGet();
                    }

                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                            log.info("News scraping job completed successfully");
                            stepExecutionMetrics.computeIfAbsent("jobs_completed", k -> new AtomicLong(0)).incrementAndGet();
                        } else {
                            log.error("News scraping job failed with status: {}", jobExecution.getStatus());
                            stepExecutionMetrics.computeIfAbsent("jobs_failed", k -> new AtomicLong(0)).incrementAndGet();
                        }
                    }
                })
                .start(newsCrawlingStep())
                .next(aiAnalysisStep())
                .build();
    }

    @Bean
    public Step newsCrawlingStep() {
        return new StepBuilder("newsCrawlingStep", jobRepository)
                .tasklet(newsCrawlingTasklet, transactionManager)
                .listener(new StepExecutionListener() {
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        log.info("Starting news crawling step");
                        stepExecutionMetrics.computeIfAbsent("crawling_started", k -> new AtomicLong(0)).incrementAndGet();
                    }

                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
                            log.info("News crawling step completed successfully");
                            stepExecutionMetrics.computeIfAbsent("crawling_completed", k -> new AtomicLong(0)).incrementAndGet();
                        } else {
                            log.error("News crawling step failed");
                            stepExecutionMetrics.computeIfAbsent("crawling_failed", k -> new AtomicLong(0)).incrementAndGet();
                        }
                        return ExitStatus.COMPLETED;
                    }
                })
                .build();
    }

    @Bean
    public Step aiAnalysisStep() {
        return new StepBuilder("aiAnalysisStep", jobRepository)
                .<NewsJpaEntity, NewsJpaEntity>chunk(10, transactionManager)
                .reader(aiAnalysisReader())
                .processor(aiAnalysisProcessor())
                .writer(aiAnalysisWriter())
                .listener(new ChunkListener() {
                    @Override
                    public void beforeChunk(ChunkContext context) {
                        log.debug("Starting AI analysis chunk processing");
                    }

                    @Override
                    public void afterChunk(ChunkContext context) {
                        log.debug("AI analysis chunk processing completed");
                        stepExecutionMetrics.computeIfAbsent("chunks_processed", k -> new AtomicLong(0)).incrementAndGet();
                    }

                    @Override
                    public void afterChunkError(ChunkContext context) {
                        log.error("AI analysis chunk processing failed");
                        stepExecutionMetrics.computeIfAbsent("chunks_failed", k -> new AtomicLong(0)).incrementAndGet();
                        errorMetrics.computeIfAbsent("ai_analysis_errors", k -> new AtomicLong(0)).incrementAndGet();
                    }
                })
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<NewsJpaEntity> aiAnalysisReader() {
        return new JpaPagingItemReaderBuilder<NewsJpaEntity>()
                .name("aiAnalysisReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT n FROM NewsJpaEntity n WHERE n.overview IS NULL")
                .pageSize(10)
                .build();
    }

    @Bean
    public ItemProcessor<NewsJpaEntity, NewsJpaEntity> aiAnalysisProcessor() {
        return newsEntity -> {
            try {
                if (newsEntity.getOverview() == null) {
                    log.debug("Processing news for AI analysis: {}", newsEntity.getId());
                    stepExecutionMetrics.computeIfAbsent("news_processed", k -> new AtomicLong(0)).incrementAndGet();
                    return newsEntity;
                }
                return null;
            } catch (Exception e) {
                log.error("Error processing news entity: {}", newsEntity.getId(), e);
                errorMetrics.computeIfAbsent("processing_errors", k -> new AtomicLong(0)).incrementAndGet();
                throw e;
            }
        };
    }

    @Bean
    public ItemWriter<NewsJpaEntity> aiAnalysisWriter() {
        return items -> {
            try {
                log.info("Writing {} processed news items", items.size());
                stepExecutionMetrics.computeIfAbsent("news_written", k -> new AtomicLong(0)).incrementAndGet();
            } catch (Exception e) {
                log.error("Error writing news items", e);
                errorMetrics.computeIfAbsent("writing_errors", k -> new AtomicLong(0)).incrementAndGet();
                throw e;
            }
        };
    }

    @Bean
    public JobParametersIncrementer jobParametersIncrementer() {
        return new RunIdIncrementer();
    }

    public BatchJobMetrics getBatchJobMetrics() {
        return new BatchJobMetrics(
                stepExecutionMetrics.entrySet().stream()
                        .collect(ConcurrentHashMap::new,
                                (map, entry) -> map.put(entry.getKey(), entry.getValue().get()),
                                ConcurrentHashMap::putAll),
                errorMetrics.entrySet().stream()
                        .collect(ConcurrentHashMap::new,
                                (map, entry) -> map.put(entry.getKey(), entry.getValue().get()),
                                ConcurrentHashMap::putAll)
        );
    }

    public void resetMetrics() {
        stepExecutionMetrics.clear();
        errorMetrics.clear();
        log.info("Batch job metrics reset");
    }

    public record BatchJobMetrics(
            ConcurrentHashMap<String, Long> stepExecutionMetrics,
            ConcurrentHashMap<String, Long> errorMetrics
    ) {
        public long getTotalProcessed() {
            return stepExecutionMetrics.getOrDefault("news_processed", 0L);
        }

        public long getTotalErrors() {
            return errorMetrics.values().stream().mapToLong(Long::longValue).sum();
        }

        public double getErrorRate() {
            long total = getTotalProcessed();
            if (total == 0) return 0.0;
            return (double) getTotalErrors() / total * 100;
        }
    }

    @Bean
    public Step djlAnalysisStep() {
        return new StepBuilder("djlAnalysisStep", jobRepository)
                .tasklet(djlAnalysisTasklet, transactionManager)
                .listener(new StepExecutionListener() {
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        log.info("DJL 감정분석 단계 시작");
                    }

                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        DjlAnalysisTasklet.DjlAnalysisMetrics metrics = djlAnalysisTasklet.getAnalysisMetrics();
                        log.info("DJL 감정분석 단계 완료 - 성공률: {:.2f}%, 평균 처리시간: {:.2f}ms",
                                metrics.getSuccessRate(), metrics.getAverageProcessingTime());
                        return ExitStatus.COMPLETED;
                    }
                })
                .build();
    }

    @Bean
    public Job newsScrapJobWithDjl() {
        return new JobBuilder("newsScrapJobWithDjl", jobRepository)
                .incrementer(jobParametersIncrementer())
                .start(newsCrawlingStep())
                .next(djlAnalysisStep())
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                        log.info("Starting DJL news scraping job: {}", jobExecution.getJobInstance().getJobName());
                    }

                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        log.info("DJL news scraping job completed with status: {}", jobExecution.getStatus());
                    }
                })
                .build();
    }

    @Bean
    public Step sentimentAnalysisStep() {
        return new StepBuilder("sentimentAnalysisStep", jobRepository)
                .tasklet(sentimentAnalysisTasklet, transactionManager)
                .listener(new StepExecutionListener() {
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        log.info("감정분석 단계 시작");
                    }

                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        var metrics = sentimentAnalysisTasklet.getAnalysisMetrics();
                        log.info("감정분석 단계 완료 - 성공률: {:.2f}%, 평균 처리시간: {:.2f}ms",
                                metrics.getSuccessRate(), metrics.getAverageProcessingTime());
                        return ExitStatus.COMPLETED;
                    }
                })
                .build();
    }

    @Bean
    public Job newsScrapJobWithSentiment() {
        return new JobBuilder("newsScrapJobWithSentiment", jobRepository)
                .incrementer(jobParametersIncrementer())
                .start(newsCrawlingStep())
                .next(sentimentAnalysisStep())
                .next(aiAnalysisStep())
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                        log.info("Starting sentiment analysis news scraping job: {}", jobExecution.getJobInstance().getJobName());
                    }

                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        log.info("Sentiment analysis news scraping job completed with status: {}", jobExecution.getStatus());
                    }
                })
                .build();
    }
}