package com.sleekydz86.finsight.core.global.logging;

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
        logger.info(objectMapper.writeValueAsString(logData));
    }

    public void logWarn(String message, Map<String, Object> context) {
        Map<String, Object> logData = createLogData("WARN", message, context);
        logger.warn(objectMapper.writeValueAsString(logData));
    }

    public void logError(String message, Map<String, Object> context, Throwable throwable) {
        Map<String, Object> logData = createLogData("ERROR", message, context);
        logData.put("exception", throwable.getClass().getSimpleName());
        logData.put("exceptionMessage", throwable.getMessage());
        logger.error(objectMapper.writeValueAsString(logData), throwable);
    }

    public void logSecurity(String message, Map<String, Object> context) {
        Map<String, Object> logData = createLogData("SECURITY", message, context);
        logger.warn(objectMapper.writeValueAsString(logData));
    }

    public void logPerformance(String operation, long duration, Map<String, Object> context) {
        Map<String, Object> logData = createLogData("PERFORMANCE", "Operation completed", context);
        logData.put("operation", operation);
        logData.put("duration", duration);
        logger.info(objectMapper.writeValueAsString(logData));
    }

    public void logBusiness(String message, Map<String, Object> context) {
        Map<String, Object> logData = createLogData("BUSINESS", message, context);
        logger.info(objectMapper.writeValueAsString(logData));
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
}