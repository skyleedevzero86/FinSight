package com.sleekydz86.finsight.core.global.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ControllerAdvice
public class BatchGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BatchGlobalExceptionHandler.class);

    private final MessageSource messageSource;

    @Autowired
    public BatchGlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("일반 예외가 발생했습니다", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "INTERNAL_SERVER_ERROR");
        response.put("message", getLocalizedMessage("user.message.server.error", locale));
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(AiAnalysisFailedException.class)
    public ResponseEntity<Map<String, Object>> handleAiAnalysisFailedException(AiAnalysisFailedException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("AI 분석에 실패했습니다", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "AI_ANALYSIS_FAILED");
        response.put("message", getLocalizedMessage("user.message.ai.analysis.failed", locale));
        response.put("model", ex.getModelName());
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(NewsScrapingFailedException.class)
    public ResponseEntity<Map<String, Object>> handleNewsScrapingFailedException(NewsScrapingFailedException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("뉴스 수집에 실패했습니다", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "NEWS_SCRAPING_FAILED");
        response.put("message", getLocalizedMessage("user.message.news.scraping.failed", locale));
        response.put("provider", ex.getProvider());
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(DatabaseConnectionException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseConnectionException(DatabaseConnectionException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("데이터베이스 연결에 실패했습니다", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "DATABASE_CONNECTION_FAILED");
        response.put("message", getLocalizedMessage("user.message.database.error", locale));
        response.put("databaseType", ex.getDatabaseType());
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<Map<String, Object>> handleExternalServiceException(ExternalServiceException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("외부 서비스 호출에 실패했습니다", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "EXTERNAL_SERVICE_FAILED");
        response.put("message", getLocalizedMessage("user.message.external.service.error", locale));
        response.put("serviceName", ex.getServiceName());
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPasswordException(InvalidPasswordException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("유효하지 않은 비밀번호입니다", ex);

        List<String> validationErrors = ex.getValidationErrors() != null
                ? ex.getValidationErrors()
                : List.of();
        String message = !validationErrors.isEmpty()
                ? String.join(" ", validationErrors)
                : getLocalizedMessage("user.message.password.invalid", locale);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "INVALID_PASSWORD");
        response.put("message", message);
        response.put("errors", validationErrors);
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(UserNotFoundException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("사용자를 찾을 수 없습니다", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "USER_NOT_FOUND");
        response.put("message", getLocalizedMessage("user.message.user.not.found", locale));
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExistsException(UserAlreadyExistsException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("이미 존재하는 사용자입니다", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "USER_ALREADY_EXISTS");
        response.put("message", getLocalizedMessage("user.message.user.already.exists", locale));
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationFailedException(AuthenticationFailedException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("인증에 실패했습니다", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "AUTHENTICATION_FAILED");
        response.put("message", getLocalizedMessage("user.message.authentication.failed", locale));
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleTokenExpiredException(TokenExpiredException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("토큰이 만료되었습니다", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "TOKEN_EXPIRED");
        response.put("message", getLocalizedMessage("user.message.token.expired", locale));
        response.put("tokenType", ex.getTokenType());
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTokenException(InvalidTokenException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("유효하지 않은 토큰입니다", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "INVALID_TOKEN");
        response.put("message", getLocalizedMessage("user.message.token.invalid", locale));
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(InsufficientPermissionException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientPermissionException(InsufficientPermissionException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("권한이 부족합니다", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "INSUFFICIENT_PERMISSION");
        response.put("message", getLocalizedMessage("user.message.insufficient.permission", locale));
        response.put("requiredPermission", ex.getRequiredPermission());
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    private Locale getLocale(HttpServletRequest request) {
        try {

            Locale locale = LocaleContextHolder.getLocale();
            if (locale != null) {
                return locale;
            }

            return request.getLocale();
        } catch (Exception e) {
            log.warn("로케일 확인에 실패하여 기본 로케일을 사용합니다", e);
            return Locale.getDefault();
        }
    }

    private String getLocalizedMessage(String code, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(code, args, code, locale);
        } catch (Exception e) {
            log.warn("코드에 대한 다국어 메시지 조회에 실패했습니다: {}", code, e);
            return code;
        }
    }
}