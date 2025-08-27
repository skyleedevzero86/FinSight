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
        log.error("Generic exception occurred", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "INTERNAL_SERVER_ERROR");
        response.put("message", getLocalizedMessage("error.generic", locale));
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(AiAnalysisFailedException.class)
    public ResponseEntity<Map<String, Object>> handleAiAnalysisFailedException(AiAnalysisFailedException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("AI analysis failed", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "AI_ANALYSIS_FAILED");
        response.put("message", getLocalizedMessage("error.ai.analysis.failed", locale, ex.getModelName(), ex.getMessage()));
        response.put("model", ex.getModelName());
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(NewsScrapingFailedException.class)
    public ResponseEntity<Map<String, Object>> handleNewsScrapingFailedException(NewsScrapingFailedException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("News scraping failed", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "NEWS_SCRAPING_FAILED");
        response.put("message", getLocalizedMessage("error.news.scraping.failed", locale, ex.getProvider(), ex.getMessage()));
        response.put("provider", ex.getProvider());
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(DatabaseConnectionException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseConnectionException(DatabaseConnectionException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("Database connection failed", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "DATABASE_CONNECTION_FAILED");
        response.put("message", getLocalizedMessage("error.database.connection.failed", locale, ex.getDatabaseType(), ex.getMessage()));
        response.put("databaseType", ex.getDatabaseType());
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<Map<String, Object>> handleExternalServiceException(ExternalServiceException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("External service call failed", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "EXTERNAL_SERVICE_FAILED");
        response.put("message", getLocalizedMessage("error.external.service.failed", locale, ex.getServiceName(), ex.getMessage()));
        response.put("serviceName", ex.getServiceName());
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPasswordException(InvalidPasswordException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("Invalid password", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "INVALID_PASSWORD");
        response.put("message", getLocalizedMessage("error.invalid.password", locale));
        response.put("errors", ex.getValidationErrors()); // getErrors() -> getValidationErrors()로 변경
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(UserNotFoundException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("User not found", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "USER_NOT_FOUND");
        response.put("message", getLocalizedMessage("error.user.not.found", locale));
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExistsException(UserAlreadyExistsException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("User already exists", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "USER_ALREADY_EXISTS");
        response.put("message", getLocalizedMessage("error.user.already.exists", locale));
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationFailedException(AuthenticationFailedException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("Authentication failed", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "AUTHENTICATION_FAILED");
        response.put("message", getLocalizedMessage("error.authentication.failed", locale));
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleTokenExpiredException(TokenExpiredException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("Token expired", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "TOKEN_EXPIRED");
        response.put("message", getLocalizedMessage("error.token.expired", locale, ex.getTokenType()));
        response.put("tokenType", ex.getTokenType());
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTokenException(InvalidTokenException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("Invalid token", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "INVALID_TOKEN");
        response.put("message", getLocalizedMessage("error.invalid.token", locale));
        response.put("timestamp", System.currentTimeMillis());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(InsufficientPermissionException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientPermissionException(InsufficientPermissionException ex, HttpServletRequest request) {
        Locale locale = getLocale(request);
        log.error("Insufficient permission", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("error", "INSUFFICIENT_PERMISSION");
        response.put("message", getLocalizedMessage("error.insufficient.permission", locale, ex.getRequiredPermission()));
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
            log.warn("Failed to resolve locale, using default", e);
            return Locale.getDefault();
        }
    }

    private String getLocalizedMessage(String code, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(code, args, code, locale);
        } catch (Exception e) {
            log.warn("Failed to get localized message for code: {}", code, e);
            return code;
        }
    }
}