package com.sleekydz86.finsight.core.global.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class StructuredLogger {

    private static final Logger logger = LoggerFactory.getLogger(StructuredLogger.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void logInfo(String message, Map<String, Object> context) {
        Map<String, Object> logData = createLogData("INFO", message, context);
        logSafely(logger::info, logData);
    }

    public void logWarn(String message, Map<String, Object> context) {
        Map<String, Object> logData = createLogData("WARN", message, context);
        logSafely(logger::warn, logData);
    }

    public void logError(String message, Map<String, Object> context, Throwable throwable) {
        Map<String, Object> logData = createLogData("ERROR", message, context);
        logData.put("exception", throwable.getClass().getSimpleName());
        logData.put("exceptionMessage", throwable.getMessage());
        logSafelyWithThrowable(logger::error, logData, throwable);
    }

    public void logSecurity(String message, Map<String, Object> context) {
        Map<String, Object> logData = createLogData("SECURITY", message, context);
        logSafely(logger::warn, logData);
    }

    public void logPerformance(String operation, long duration, Map<String, Object> context) {
        Map<String, Object> logData = createLogData("PERFORMANCE", "Operation completed", context);
        logData.put("operation", operation);
        logData.put("duration", duration);
        logSafely(logger::info, logData);
    }

    public void logBusiness(String message, Map<String, Object> context) {
        Map<String, Object> logData = createLogData("BUSINESS", message, context);
        logSafely(logger::info, logData);
    }

    private void logSafely(LogFunction logFunction, Map<String, Object> logData) {
        try {
            String jsonString = objectMapper.writeValueAsString(logData);
            logFunction.log(jsonString);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to convert log data to JSON: {}", e.getMessage());
            logFunction.log(logData.toString());
        }
    }

    private void logSafelyWithThrowable(LogFunctionWithThrowable logFunction, Map<String, Object> logData, Throwable throwable) {
        try {
            String jsonString = objectMapper.writeValueAsString(logData);
            logFunction.log(jsonString, throwable);
        } catch (JsonProcessingException e) {
            // JSON 변환 실패 시 기본 로깅으로 폴백
            logger.warn("Failed to convert log data to JSON: {}", e.getMessage());
            logFunction.log(logData.toString(), throwable);
        }
    }

    private Map<String, Object> createLogData(String level, String message, Map<String, Object> context) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", LocalDateTime.now().toString());
        logData.put("level", level);
        logData.put("message", message);
        logData.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        logData.put("application", "finsight");
        logData.put("version", "1.0.0");

        if (context != null) {
            logData.putAll(context);
        }

        return logData;
    }

    @FunctionalInterface
    private interface LogFunction {
        void log(String message);
    }

    @FunctionalInterface
    private interface LogFunctionWithThrowable {
        void log(String message, Throwable throwable);
    }
}