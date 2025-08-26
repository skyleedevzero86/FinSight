package com.sleekydz86.finsight.batch.global.exception;

import com.sleekydz86.finsight.core.global.exception.BaseException;
import com.sleekydz86.finsight.core.global.exception.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class BatchGlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(BatchGlobalExceptionHandler.class);

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // Spring Batch 관련 예외 처리
    @ExceptionHandler(JobExecutionException.class)
    public ResponseEntity<ErrorResponse> handleJobExecutionException(JobExecutionException ex) {
        logger.error("JobExecutionException occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("BATCH_001")
                .errorType("Job Execution Error")
                .message("배치 작업 실행 중 오류가 발생했습니다")
                .userMessage("배치 작업 처리 중 오류가 발생했습니다. 관리자에게 문의해주세요.")
                .httpStatus(500)
                .timestamp(LocalDateTime.now())
                .additionalInfo(Map.of("jobName", ex.getJobName(), "exitCode", ex.getExitCode()))
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(StepExecutionException.class)
    public ResponseEntity<ErrorResponse> handleStepExecutionException(StepExecutionException ex) {
        logger.error("StepExecutionException occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("BATCH_002")
                .errorType("Step Execution Error")
                .message("배치 단계 실행 중 오류가 발생했습니다")
                .userMessage("배치 작업 단계 처리 중 오류가 발생했습니다. 관리자에게 문의해주세요.")
                .httpStatus(500)
                .timestamp(LocalDateTime.now())
                .additionalInfo(Map.of("stepName", ex.getStepName(), "exitCode", ex.getExitCode()))
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(JobExecutionAlreadyRunningException.class)
    public ResponseEntity<ErrorResponse> handleJobExecutionAlreadyRunningException(JobExecutionAlreadyRunningException ex) {
        logger.warn("JobExecutionAlreadyRunningException occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("BATCH_003")
                .errorType("Job Already Running")
                .message("배치 작업이 이미 실행 중입니다")
                .userMessage("해당 배치 작업이 이미 실행 중입니다. 잠시 후 다시 시도해주세요.")
                .httpStatus(409)
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(409)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(JobInstanceAlreadyCompleteException.class)
    public ResponseEntity<ErrorResponse> handleJobInstanceAlreadyCompleteException(JobInstanceAlreadyCompleteException ex) {
        logger.warn("JobInstanceAlreadyCompleteException occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("BATCH_004")
                .errorType("Job Already Complete")
                .message("배치 작업이 이미 완료되었습니다")
                .userMessage("해당 배치 작업은 이미 완료되었습니다.")
                .httpStatus(409)
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(409)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(JobRestartException.class)
    public ResponseEntity<ErrorResponse> handleJobRestartException(JobRestartException ex) {
        logger.error("JobRestartException occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("BATCH_005")
                .errorType("Job Restart Error")
                .message("배치 작업 재시작에 실패했습니다")
                .userMessage("배치 작업 재시작 중 오류가 발생했습니다. 관리자에게 문의해주세요.")
                .httpStatus(500)
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // 배치 아이템 처리 관련 예외
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        logger.warn("ValidationException occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("BATCH_006")
                .errorType("Batch Validation Error")
                .message("배치 데이터 검증에 실패했습니다")
                .userMessage("배치 데이터 검증 중 오류가 발생했습니다. 데이터를 확인해주세요.")
                .httpStatus(400)
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // 커스텀 예외 처리
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        logger.error("BaseException occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .errorType(ex.getErrorType())
                .message(ex.getMessage())
                .userMessage("배치 작업 처리 중 오류가 발생했습니다. 관리자에게 문의해주세요.")
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(ex.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // 데이터베이스 관련 예외 처리
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex) {
        logger.error("DataAccessException occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("BATCH_DB_001")
                .errorType("Batch Database Error")
                .message("배치 작업 중 데이터베이스 오류가 발생했습니다")
                .userMessage("배치 작업 중 데이터 처리 오류가 발생했습니다. 관리자에게 문의해주세요.")
                .httpStatus(500)
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // 일반적인 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected exception occurred in batch: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("BATCH_999")
                .errorType("Batch Internal Error")
                .message("배치 작업 중 내부 오류가 발생했습니다")
                .userMessage("배치 작업 처리 중 오류가 발생했습니다. 관리자에게 문의해주세요.")
                .httpStatus(500)
                .timestamp(LocalDateTime.now())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}