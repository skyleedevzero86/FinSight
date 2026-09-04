package com.sleekydz86.finsight.batch.board.tasklet;

import com.sleekydz86.finsight.core.board.domain.BoardModerationRun;
import com.sleekydz86.finsight.core.board.domain.port.in.BoardBatchModerationUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class BoardModerationTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(BoardModerationTasklet.class);

    private static final String PARAM_REPORT_THRESHOLD = "reportThreshold";

    private final BoardBatchModerationUseCase boardBatchModerationUseCase;

    public BoardModerationTasklet(BoardBatchModerationUseCase boardBatchModerationUseCase) {
        this.boardBatchModerationUseCase = boardBatchModerationUseCase;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        JobParameters params = contribution.getStepExecution().getJobExecution().getJobParameters();
        Long raw = params.getLong(PARAM_REPORT_THRESHOLD);
        int threshold = raw != null ? raw.intValue() : 5;

        BoardModerationRun run = boardBatchModerationUseCase.hideOverReportedActiveBoards(
                threshold, "BATCH", null);
        contribution.incrementWriteCount(run.hiddenCount());
        log.info(
                "게시판 자동 숨김 처리 - runId={}, 신고 임계값: {}, 숨김 건수: {}",
                run.id(),
                threshold,
                run.hiddenCount());
        return RepeatStatus.FINISHED;
    }
}
