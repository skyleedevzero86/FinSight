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
     * Constructs a NewsScrapJobConfig with the required batch infrastructure.
     *
     * Initializes the configuration with the provided JobRepository, PlatformTransactionManager,
     * and JobLauncher used to build and execute the scheduled news-scraping job.
     */
    public NewsScrapJobConfig(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              JobLauncher jobLauncher) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.jobLauncher = jobLauncher;
    }

    /**
     * Scheduled entry point that launches the configured news scraping job.
     *
     * <p>Builds JobParameters containing a single long parameter "time" set to the current system time
     * and invokes the NewsScrap job bean. The method is scheduled at a fixed rate (FIFTY_MINUTE).
     *
     * @throws Exception if job launching fails (propagates exceptions from JobLauncher)
     */
    @Scheduled(fixedRate = FIFTY_MINUTE)
    public void runNewsJob() throws Exception {
        var jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(newsScrapJob(), jobParameters);
    }

    /**
     * Creates the Spring Batch Job bean named "newsScrapJob".
     *
     * <p>The job executes a three-step pipeline in sequence:
     * 1) newsCollectionStep, 2) aiAnalysisStep, and 3) dataSaveStep. Each step is defined as
     * a chunk-oriented step and must be provided (readers/processors/writers are configured on the step beans).
     *
     * @return a configured Job that runs the news collection, AI analysis, and data persistence steps in order
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
     * Declares the "newsCollectionStep" Spring Batch Step bean that performs chunk-oriented collection of raw news items.
     *
     * <p>The step is configured with input type {@code Void} and output type {@code String}, uses a chunk size defined
     * by {@code MAXIMUM_CRAWLING_DATA_CHUNK_SIZE}, and participates in transactions via the injected
     * {@code PlatformTransactionManager}.</p>
     *
     * <p>Reader, processor, and writer are currently placeholders (null) and should be wired with actual implementations
     * before the job is executed.</p>
     *
     * @return the configured {@link Step} instance for the news collection phase
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
     * Declares the "aiAnalysisStep" Step bean: a chunk-oriented Spring Batch step that
     * performs AI analysis over scraped news items.
     *
     * <p>The step processes items of type {@code String} and produces {@code String}
     * outputs using a chunk size defined by {@code MAXIMUM_CRAWLING_DATA_CHUNK_SIZE}
     * and the configured {@code transactionManager}. Reader, processor and writer are
     * left as {@code null} placeholders and must be provided (wired) before the step
     * is used in a running job.
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
     * Creates the "dataSaveStep" Step bean for the job: a chunk-oriented step that reads and writes String items.
     *
     * Configured with a chunk size of MAXIMUM_CRAWLING_DATA_CHUNK_SIZE and the injected transaction manager.
     * The reader and writer are currently null placeholders and must be supplied before executing the job.
     *
     * @return the configured Step named "dataSaveStep"
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