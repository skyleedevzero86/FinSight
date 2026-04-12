package com.sleekydz86.finsight.batch.board.job;

import com.sleekydz86.finsight.batch.board.tasklet.BoardModerationTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BoardModerationJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BoardModerationTasklet boardModerationTasklet;

    public BoardModerationJobConfig(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            BoardModerationTasklet boardModerationTasklet) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.boardModerationTasklet = boardModerationTasklet;
    }

    @Bean
    public Step boardModerationStep() {
        return new StepBuilder("boardModerationStep", jobRepository)
                .tasklet(boardModerationTasklet, transactionManager)
                .build();
    }

    @Bean
    public Job boardModerationJob() {
        return new JobBuilder("boardModerationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(boardModerationStep())
                .build();
    }
}
