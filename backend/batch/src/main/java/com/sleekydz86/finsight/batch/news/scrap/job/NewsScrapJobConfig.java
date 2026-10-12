package com.sleekydz86.finsight.batch.news.scrap.job;

import com.sleekydz86.finsight.batch.news.scrap.tasklet.DjlAnalysisTasklet;
import com.sleekydz86.finsight.batch.news.scrap.tasklet.NewsCrawlingTasklet;
import com.sleekydz86.finsight.batch.news.scrap.tasklet.NewsEmbeddingTasklet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class NewsScrapJobConfig {

    private static final Logger log = LoggerFactory.getLogger(NewsScrapJobConfig.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final NewsCrawlingTasklet newsCrawlingTasklet;
    private final DjlAnalysisTasklet djlAnalysisTasklet;
    private final NewsEmbeddingTasklet newsEmbeddingTasklet;

    private final ConcurrentHashMap<String, AtomicLong> stepExecutionMetrics = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> errorMetrics = new ConcurrentHashMap<>();

    public NewsScrapJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            NewsCrawlingTasklet newsCrawlingTasklet,
            DjlAnalysisTasklet djlAnalysisTasklet,
            NewsEmbeddingTasklet newsEmbeddingTasklet) {

        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.newsCrawlingTasklet = newsCrawlingTasklet;
        this.djlAnalysisTasklet = djlAnalysisTasklet;
        this.newsEmbeddingTasklet = newsEmbeddingTasklet;
    }

    @Bean
    public Job newsScrapJob() {
        return new JobBuilder("newsScrapJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) {
                        log.info("뉴스 스크래핑 작업 시작: {}", jobExecution.getJobInstance().getJobName());
                        stepExecutionMetrics.computeIfAbsent("jobs_started", k -> new AtomicLong(0)).incrementAndGet();
                    }

                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                            log.info("뉴스 스크래핑 작업이 성공적으로 완료되었습니다");
                            stepExecutionMetrics.computeIfAbsent("jobs_completed", k -> new AtomicLong(0)).incrementAndGet();
                        } else {
                            log.error("뉴스 스크래핑 작업 실패 - 상태: {}", jobExecution.getStatus());
                            stepExecutionMetrics.computeIfAbsent("jobs_failed", k -> new AtomicLong(0)).incrementAndGet();
                        }
                    }
                })
                .start(newsCrawlingStep())
                .next(djlAnalysisStep())
                .next(newsEmbeddingStep())
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
                        stepExecutionMetrics.computeIfAbsent("crawling_started", k -> new AtomicLong(0)).incrementAndGet();
                    }

                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
                            log.info("뉴스 크롤링 단계가 성공적으로 완료되었습니다");
                            stepExecutionMetrics.computeIfAbsent("crawling_completed", k -> new AtomicLong(0)).incrementAndGet();
                        } else {
                            log.error("뉴스 크롤링 단계 실패");
                            stepExecutionMetrics.computeIfAbsent("crawling_failed", k -> new AtomicLong(0)).incrementAndGet();
                        }
                        return null;
                    }
                })
                .build();
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
        log.info("배치 작업 지표 초기화 완료");
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
                        return null;
                    }
                })
                .build();
    }

    @Bean
    public Step newsEmbeddingStep() {
        return new StepBuilder("newsEmbeddingStep", jobRepository)
                .tasklet(newsEmbeddingTasklet, transactionManager)
                .build();
    }
}