package com.sleekydz86.finsight.core.global.security;

import com.sleekydz86.finsight.core.global.logging.StructuredLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SecurityAuditService {

    @Autowired
    private StructuredLogger structuredLogger;

    private final ConcurrentHashMap<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastAttempt = new ConcurrentHashMap<>();

    public void logLoginAttempt(String email, String ipAddress, boolean success) {
        Map<String, Object> context = new HashMap<>();
        context.put("email", email);
        context.put("ipAddress", ipAddress);
        context.put("success", success);
        context.put("timestamp", LocalDateTime.now());

        if (success) {
            structuredLogger.logInfo("Login successful", context);
            resetFailedAttempts(email);
        } else {
            structuredLogger.logSecurity("Login failed", context);
            incrementFailedAttempts(email);
        }
    }

    public void logPasswordChange(String email, String ipAddress) {
        Map<String, Object> context = new HashMap<>();
        context.put("email", email);
        context.put("ipAddress", ipAddress);
        context.put("action", "password_change");
        context.put("timestamp", LocalDateTime.now());

        structuredLogger.logSecurity("Password changed", context);
    }

    public void logSuspiciousActivity(String email, String ipAddress, String activity, String reason) {
        Map<String, Object> context = new HashMap<>();
        context.put("email", email);
        context.put("ipAddress", ipAddress);
        context.put("activity", activity);
        context.put("reason", reason);
        context.put("timestamp", LocalDateTime.now());

        structuredLogger.logSecurity("Suspicious activity detected", context);
    }

    public void logDataAccess(String email, String resource, String action, boolean success) {
        Map<String, Object> context = new HashMap<>();
        context.put("email", email);
        context.put("resource", resource);
        context.put("action", action);
        context.put("success", success);
        context.put("timestamp", LocalDateTime.now());

        structuredLogger.logSecurity("Data access", context);
    }

    public void logApiAccess(HttpServletRequest request, String email, int statusCode, long duration) {
        Map<String, Object> context = new HashMap<>();
        context.put("email", email);
        context.put("method", request.getMethod());
        context.put("uri", request.getRequestURI());
        context.put("ipAddress", getClientIpAddress(request));
        context.put("userAgent", request.getHeader("User-Agent"));
        context.put("statusCode", statusCode);
        context.put("duration", duration);
        context.put("timestamp", LocalDateTime.now());

        structuredLogger.logInfo("API access", context);
    }

    public boolean isAccountLocked(String email) {
        AtomicInteger attempts = failedAttempts.get(email);
        return attempts != null && attempts.get() >= 5;
    }

    public int getFailedAttempts(String email) {
        AtomicInteger attempts = failedAttempts.get(email);
        return attempts != null ? attempts.get() : 0;
    }

    private void incrementFailedAttempts(String email) {
        failedAttempts.computeIfAbsent(email, k -> new AtomicInteger(0)).incrementAndGet();
        lastAttempt.put(email, LocalDateTime.now());
    }

    private void resetFailedAttempts(String email) {
        failedAttempts.remove(email);
        lastAttempt.remove(email);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}