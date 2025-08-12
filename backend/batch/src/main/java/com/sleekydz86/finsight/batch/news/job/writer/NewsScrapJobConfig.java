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

    /**
     * Creates a NewsScrapJobConfig using the provided JobRepository, PlatformTransactionManager, and JobLauncher.
     */
    public NewsScrapJobConfig(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              JobLauncher jobLauncher) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.jobLauncher = jobLauncher;
    }

    /**
     * Triggers the configured Spring Batch job that performs news scraping and processing.
     *
     * This method is scheduled to run at a fixed rate defined by FIFTY_MINUTE and launches
     * the job with a time-based JobParameter ("time") set to the current system time to
     * ensure a unique job instance for each invocation.
     *
     * @throws Exception if the job launch or execution fails
     */
    @Scheduled(fixedRate = FIFTY_MINUTE)
    public void runNewsJob() throws Exception {
        var jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(newsScrapJob(), jobParameters);
    }

    /**
     * Creates and exposes the Spring Batch Job bean "newsScrapJob".
     *
     * <p>The job executes three steps in sequence: newsCollectionStep -> aiAnalysisStep -> dataSaveStep.
     *
     * @return a Job configured to run the news collection, AI analysis, and data saving steps in order
     */
    @Bean
    public Job newsScrapJob() {
        return new JobBuilder("newsScrapJob", jobRepository)
                .start(newsCollectionStep())
                .next(aiAnalysisStep())
                .next(dataSaveStep())
                .build();
    }

    /**
     * Spring Batch Step bean that performs the "news collection" chunk-oriented step.
     *
     * <p>Configured with generics <code>&lt;Void, String&gt;</code> and a chunk size of
     * {@code MAXIMUM_CRAWLING_DATA_CHUNK_SIZE}. The step is built using the injected
     * JobRepository and PlatformTransactionManager. Reader, processor and writer are
     * currently unset (placeholders) and should be provided before executing the job.
     *
     * @return a configured {@link org.springframework.batch.core.Step} for collecting news items
     */
    @Bean
    public Step newsCollectionStep() {
        return new StepBuilder("newsCollectionStep", jobRepository)
                .<Void, String>chunk(MAXIMUM_CRAWLING_DATA_CHUNK_SIZE, transactionManager)
                .reader(null)
                .processor(null)
                .writer(null)
                .build();
    }

    /**
     * Declares the "aiAnalysisStep" Spring Batch step as a bean.
     *
     * <p>Returns a chunk-oriented Step with item types {@code String} -> {@code String} and a chunk
     * size of {@code MAXIMUM_CRAWLING_DATA_CHUNK_SIZE}, managed by the injected transaction manager.
     * The step is built with the application {@code jobRepository}.
     *
     * <p>Note: reader, processor, and writer are currently set to {@code null} (placeholders) and must
     * be supplied for the step to execute correctly.
     *
     * @return a configured {@link org.springframework.batch.core.Step} named "aiAnalysisStep"
     */
    @Bean
    public Step aiAnalysisStep() {
        return new StepBuilder("aiAnalysisStep", jobRepository)
                .<String, String>chunk(MAXIMUM_CRAWLING_DATA_CHUNK_SIZE, transactionManager)
                .reader(null)
                .processor(null)
                .writer(null)
                .build();
    }

    /**
     * Spring Batch Step bean that persists processed news items.
     *
     * <p>Defines a chunk-oriented step named "dataSaveStep" with input/output types of
     * String and a chunk size of MAXIMUM_CRAWLING_DATA_CHUNK_SIZE. The step is created
     * using the configured JobRepository and PlatformTransactionManager.
     *
     * <p>Reader and writer are currently placeholders (null) and must be provided for the
     * step to perform actual I/O.
     *
     * @return a configured Step instance named "dataSaveStep"
     */
    @Bean
    public Step dataSaveStep() {
        return new StepBuilder("dataSaveStep", jobRepository)
                .<String, String>chunk(MAXIMUM_CRAWLING_DATA_CHUNK_SIZE, transactionManager)
                .reader(null)
                .writer(null)
                .build();
    }
}