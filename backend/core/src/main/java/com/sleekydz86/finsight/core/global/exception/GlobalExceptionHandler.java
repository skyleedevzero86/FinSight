package com.sleekydz86.finsight.core.global.exception;

import com.sleekydz86.finsight.core.global.exception.dto.ErrorResponse;
import com.sleekydz86.finsight.core.global.exception.dto.ValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private HttpServletRequest request;

    // 커스텀 예외 처리
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, WebRequest request) {
        logger.error("BaseException occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .errorType(ex.getErrorType())
                .message(ex.getMessage())
                .userMessage(getUserFriendlyMessage(ex.getErrorCode()))
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(ex.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // 사용자 관련 예외 처리
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex, WebRequest request) {
        logger.warn("UserNotFoundException occurred: userId={}", ex.getUserId());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .errorType(ex.getErrorType())
                .message(ex.getMessage())
                .userMessage("요청하신 사용자 정보를 찾을 수 없습니다. 다시 확인해주세요.")
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(ex.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex, WebRequest request) {
        logger.warn("UserAlreadyExistsException occurred: email={}", ex.getEmail());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .errorType(ex.getErrorType())
                .message(ex.getMessage())
                .userMessage("이미 가입된 이메일입니다. 다른 이메일을 사용하거나 로그인해주세요.")
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(ex.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasswordException(InvalidPasswordException ex, WebRequest request) {
        logger.warn("InvalidPasswordException occurred: validationErrors={}", ex.getValidationErrors());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .errorType(ex.getErrorType())
                .message(ex.getMessage())
                .userMessage("비밀번호가 보안 요구사항을 충족하지 않습니다. 다음 사항을 확인해주세요: " +
                        String.join(", ", ex.getValidationErrors()))
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .validationErrors(ex.getValidationErrors())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(ex.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // 인증 관련 예외 처리
    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailedException(AuthenticationFailedException ex, WebRequest request) {
        logger.warn("AuthenticationFailedException occurred: email={}", ex.getEmail());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .errorType(ex.getErrorType())
                .message(ex.getMessage())
                .userMessage("로그인에 실패했습니다. 이메일과 비밀번호를 다시 확인해주세요.")
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(ex.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpiredException(TokenExpiredException ex, WebRequest request) {
        logger.warn("TokenExpiredException occurred: tokenType={}", ex.getTokenType());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .errorType(ex.getErrorType())
                .message(ex.getMessage())
                .userMessage("로그인 세션이 만료되었습니다. 다시 로그인해주세요.")
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(ex.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTokenException(InvalidTokenException ex, WebRequest request) {
        logger.warn("InvalidTokenException occurred: tokenType={}", ex.getTokenType());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .errorType(ex.getErrorType())
                .message(ex.getMessage())
                .userMessage("유효하지 않은 인증 정보입니다. 다시 로그인해주세요.")
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(ex.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // 뉴스 관련 예외 처리
    @ExceptionHandler(NewsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNewsNotFoundException(NewsNotFoundException ex, WebRequest request) {
        logger.warn("NewsNotFoundException occurred: newsId={}", ex.getNewsId());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .errorType(ex.getErrorType())
                .message(ex.getMessage())
                .userMessage("요청하신 뉴스 정보를 찾을 수 없습니다.")
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(ex.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(NewsScrapingFailedException.class)
    public ResponseEntity<ErrorResponse> handleNewsScrapingFailedException(NewsScrapingFailedException ex, WebRequest request) {
        logger.error("NewsScrapingFailedException occurred: provider={}, reason={}", ex.getProvider(), ex.getReason());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .errorType(ex.getErrorType())
                .message(ex.getMessage())
                .userMessage("뉴스 수집 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .additionalInfo(Map.of("provider", ex.getProvider(), "reason", ex.getReason()))
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(ex.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(AiAnalysisFailedException.class)
    public ResponseEntity<ErrorResponse> handleAiAnalysisFailedException(AiAnalysisFailedException ex, WebRequest request) {
        logger.error("AiAnalysisFailedException occurred: model={}, reason={}", ex.getModel(), ex.getReason());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .errorType(ex.getErrorType())
                .message(ex.getMessage())
                .userMessage("AI 분석 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .additionalInfo(Map.of("model", ex.getModel(), "reason", ex.getReason()))
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(ex.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // 권한 관련 예외 처리
    @ExceptionHandler(InsufficientPermissionException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientPermissionException(InsufficientPermissionException ex, WebRequest request) {
        logger.warn("InsufficientPermissionException occurred: requiredRole={}, currentRole={}",
                ex.getRequiredRole(), ex.getCurrentRole());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .errorType(ex.getErrorType())
                .message(ex.getMessage())
                .userMessage("이 작업을 수행할 권한이 없습니다. 관리자에게 문의해주세요.")
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .additionalInfo(Map.of("requiredRole", ex.getRequiredRole(), "currentRole", ex.getCurrentRole()))
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(ex.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // 시스템 관련 예외 처리
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceException(ExternalServiceException ex, WebRequest request) {
        logger.error("ExternalServiceException occurred: service={}, endpoint={}", ex.getServiceName(), ex.getEndpoint());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .errorType(ex.getErrorType())
                .message(ex.getMessage())
                .userMessage("외부 서비스 연결에 문제가 발생했습니다. 잠시 후 다시 시도해주세요.")
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .additionalInfo(Map.of("serviceName", ex.getServiceName(), "endpoint", ex.getEndpoint()))
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(ex.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(DatabaseConnectionException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseConnectionException(DatabaseConnectionException ex, WebRequest request) {
        logger.error("DatabaseConnectionException occurred: databaseType={}", ex.getDatabaseType());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode())
                .errorType(ex.getErrorType())
                .message(ex.getMessage())
                .userMessage("데이터베이스 연결에 문제가 발생했습니다. 잠시 후 다시 시도해주세요.")
                .httpStatus(ex.getHttpStatus())
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .additionalInfo(Map.of("databaseType", ex.getDatabaseType()))
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(ex.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // Spring Security 예외 처리
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        logger.warn("AccessDeniedException occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("SEC_001")
                .errorType("Access Denied")
                .message("접근이 거부되었습니다")
                .userMessage("이 리소스에 접근할 권한이 없습니다.")
                .httpStatus(403)
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(403)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        logger.warn("AuthenticationException occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("SEC_002")
                .errorType("Authentication Failed")
                .message("인증에 실패했습니다")
                .userMessage("로그인이 필요합니다.")
                .httpStatus(401)
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(401)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        logger.warn("BadCredentialsException occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("SEC_003")
                .errorType("Bad Credentials")
                .message("잘못된 인증 정보입니다")
                .userMessage("이메일 또는 비밀번호가 올바르지 않습니다.")
                .httpStatus(401)
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(401)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // Spring Validation 예외 처리
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status,
                                                                  WebRequest request) {
        logger.warn("MethodArgumentNotValidException occurred: {}", ex.getMessage());

        List<ValidationErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::mapToFieldError)
                .collect(Collectors.toList());

        ValidationErrorResponse errorResponse = ValidationErrorResponse.builder()
                .errorCode("VAL_001")
                .errorType("Validation Error")
                .message("입력 데이터 검증에 실패했습니다")
                .userMessage("입력하신 정보를 다시 확인해주세요.")
                .httpStatus(400)
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .fieldErrors(fieldErrors)
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        logger.warn("ConstraintViolationException occurred: {}", ex.getMessage());

        List<ValidationErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(this::mapToFieldError)
                .collect(Collectors.toList());

        ValidationErrorResponse errorResponse = ValidationErrorResponse.builder()
                .errorCode("VAL_002")
                .errorType("Validation Error")
                .message("입력 데이터 제약 조건 위반")
                .userMessage("입력하신 정보가 올바르지 않습니다.")
                .httpStatus(400)
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .fieldErrors(fieldErrors)
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ValidationErrorResponse> handleBindException(BindException ex, WebRequest request) {
        logger.warn("BindException occurred: {}", ex.getMessage());

        List<ValidationErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::mapToFieldError)
                .collect(Collectors.toList());

        ValidationErrorResponse errorResponse = ValidationErrorResponse.builder()
                .errorCode("VAL_003")
                .errorType("Binding Error")
                .message("데이터 바인딩에 실패했습니다")
                .userMessage("입력하신 정보를 다시 확인해주세요.")
                .httpStatus(400)
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .fieldErrors(fieldErrors)
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // HTTP 메시지 관련 예외 처리
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status,
                                                                  WebRequest request) {
        logger.warn("HttpMessageNotReadableException occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("HTTP_001")
                .errorType("Message Not Readable")
                .message("요청 메시지를 읽을 수 없습니다")
                .userMessage("전송하신 데이터 형식이 올바르지 않습니다.")
                .httpStatus(400)
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // HTTP 메서드 관련 예외 처리
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                         HttpHeaders headers,
                                                                         HttpStatus status,
                                                                         WebRequest request) {
        logger.warn("HttpRequestMethodNotSupportedException occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("HTTP_002")
                .errorType("Method Not Supported")
                .message("지원하지 않는 HTTP 메서드입니다")
                .userMessage("잘못된 요청 방식입니다.")
                .httpStatus(405)
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .additionalInfo(Map.of("supportedMethods", ex.getSupportedMethods()))
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(405)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpHeaders headers,
                                                                     HttpStatus status,
                                                                     WebRequest request) {
        logger.warn("HttpMediaTypeNotSupportedException occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("HTTP_003")
                .errorType("Media Type Not Supported")
                .message("지원하지 않는 미디어 타입입니다")
                .userMessage("지원하지 않는 데이터 형식입니다.")
                .httpStatus(415)
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .additionalInfo(Map.of("supportedMediaTypes", ex.getSupportedMediaTypes()))
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(415)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // 파라미터 관련 예외 처리
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers,
                                                                          HttpStatus status,
                                                                          WebRequest request) {
        logger.warn("MissingServletRequestParameterException occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("PARAM_001")
                .errorType("Missing Parameter")
                .message("필수 파라미터가 누락되었습니다")
                .userMessage("필수 정보가 누락되었습니다.")
                .httpStatus(400)
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .additionalInfo(Map.of("parameterName", ex.getParameterName(), "parameterType", ex.getParameterType()))
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        logger.warn("MethodArgumentTypeMismatchException occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("PARAM_002")
                .errorType("Parameter Type Mismatch")
                .message("파라미터 타입이 일치하지 않습니다")
                .userMessage("입력하신 정보의 형식이 올바르지 않습니다.")
                .httpStatus(400)
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .additionalInfo(Map.of("parameterName", ex.getName(), "expectedType", ex.getRequiredType().getSimpleName()))
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(400)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // 데이터베이스 관련 예외 처리
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex, WebRequest request) {
        logger.error("DataAccessException occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("DB_001")
                .errorType("Database Access Error")
                .message("데이터베이스 접근 중 오류가 발생했습니다")
                .userMessage("데이터 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                .httpStatus(500)
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex, WebRequest request) {
        logger.error("DataIntegrityViolationException occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("DB_002")
                .errorType("Data Integrity Violation")
                .message("데이터 무결성 제약 조건 위반")
                .userMessage("입력하신 정보가 이미 존재하거나 중복됩니다.")
                .httpStatus(409)
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(409)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorResponse> handleSQLException(SQLException ex, WebRequest request) {
        logger.error("SQLException occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("DB_003")
                .errorType("SQL Error")
                .message("SQL 실행 중 오류가 발생했습니다")
                .userMessage("데이터 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                .httpStatus(500)
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .additionalInfo(Map.of("sqlState", ex.getSQLState(), "errorCode", ex.getErrorCode()))
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // 일반적인 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        logger.error("Unexpected exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("SYS_999")
                .errorType("Internal Server Error")
                .message("내부 서버 오류가 발생했습니다")
                .userMessage("서버에 문제가 발생했습니다. 잠시 후 다시 시도해주세요.")
                .httpStatus(500)
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // 404 에러 처리
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex,
                                                                   HttpHeaders headers,
                                                                   HttpStatus status,
                                                                   WebRequest request) {
        logger.warn("NoHandlerFoundException occurred: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("HTTP_404")
                .errorType("Not Found")
                .message("요청한 리소스를 찾을 수 없습니다")
                .userMessage("요청하신 페이지를 찾을 수 없습니다.")
                .httpStatus(404)
                .timestamp(LocalDateTime.now())
                .path(getRequestPath())
                .method(getRequestMethod())
                .traceId(generateTraceId())
                .build();

        return ResponseEntity.status(404)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    // 헬퍼 메서드들
    private ValidationErrorResponse.FieldError mapToFieldError(FieldError fieldError) {
        return ValidationErrorResponse.FieldError.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .userMessage(getUserFriendlyFieldMessage(fieldError.getField(), fieldError.getDefaultMessage()))
                .rejectedValue(fieldError.getRejectedValue())
                .errorCode(fieldError.getCode())
                .build();
    }

    private ValidationErrorResponse.FieldError mapToFieldError(ConstraintViolation<?> violation) {
        return ValidationErrorResponse.FieldError.builder()
                .field(violation.getPropertyPath().toString())
                .message(violation.getMessage())
                .userMessage(getUserFriendlyFieldMessage(violation.getPropertyPath().toString(), violation.getMessage()))
                .rejectedValue(violation.getInvalidValue())
                .errorCode(violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
                .build();
    }

    private String getUserFriendlyMessage(String errorCode) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage("error." + errorCode, null,
                    "오류가 발생했습니다. 다시 시도해주세요.", locale);
        } catch (Exception e) {
            return "오류가 발생했습니다. 다시 시도해주세요.";
        }
    }

    private String getUserFriendlyFieldMessage(String field, String message) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            String fieldName = messageSource.getMessage("field." + field, null, field, locale);
            return fieldName + ": " + message;
        } catch (Exception e) {
            return field + ": " + message;
        }
    }

    private String getRequestPath() {
        return request != null ? request.getContextPath() + request.getRequestURI() : "";
    }

    private String getRequestMethod() {
        return request != null ? request.getMethod() : "";
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}