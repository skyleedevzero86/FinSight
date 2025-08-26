package com.sleekydz86.finsight.core.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SecurityAuditFilter extends OncePerRequestFilter {

    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditFilter.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final AtomicLong requestCounter = new AtomicLong(0);

    private static final ConcurrentHashMap<String, IpRequestPattern> ipPatterns = new ConcurrentHashMap<>();

    private static final int SUSPICIOUS_REQUEST_THRESHOLD = 100;
    private static final int SUSPICIOUS_ERROR_THRESHOLD = 20;
    private static final long SUSPICIOUS_TIME_WINDOW = 60000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String requestId = generateRequestId();
        String clientIp = getClientIpAddress(request);

        logRequestStart(request, requestId, clientIp);

        analyzeIpPattern(clientIp, request);

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);

            long duration = System.currentTimeMillis() - startTime;
            logRequestEnd(wrappedRequest, wrappedResponse, requestId, duration, clientIp);

            detectSuspiciousPatterns(clientIp, request, response, duration);

        } catch (Exception e) {
            logSecurityException(wrappedRequest, requestId, e, clientIp);
            throw e;
        } finally {
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequestStart(HttpServletRequest request, String requestId, String clientIp) {
        try {
            SecurityAuditEvent event = SecurityAuditEvent.builder()
                    .timestamp(LocalDateTime.now().format(DATE_FORMATTER))
                    .requestId(requestId)
                    .eventType("REQUEST_START")
                    .clientIp(clientIp)
                    .method(request.getMethod())
                    .uri(request.getRequestURI())
                    .queryString(request.getQueryString())
                    .userAgent(request.getHeader("User-Agent"))
                    .referer(request.getHeader("Referer"))
                    .contentType(request.getContentType())
                    .contentLength(request.getContentLength())
                    .build();

            auditLogger.info("SECURITY_AUDIT: {}", new ObjectMapper().writeValueAsString(event));

        } catch (Exception e) {
            logger.error("보안 감사 로깅 실패", e);
        }
    }

    private void logRequestEnd(HttpServletRequest request, HttpServletResponse response,
                               String requestId, long duration, String clientIp) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            SecurityAuditEvent event = SecurityAuditEvent.builder()
                    .timestamp(LocalDateTime.now().format(DATE_FORMATTER))
                    .requestId(requestId)
                    .eventType("REQUEST_END")
                    .clientIp(clientIp)
                    .method(request.getMethod())
                    .uri(request.getRequestURI())
                    .statusCode(response.getStatus())
                    .duration(duration)
                    .username(authentication != null ? authentication.getName() : "anonymous")
                    .authorities(authentication != null ? authentication.getAuthorities().toString() : "none")
                    .build();

            auditLogger.info("SECURITY_AUDIT: {}", new ObjectMapper().writeValueAsString(event));

        } catch (Exception e) {
            logger.error("보안 감사 로깅 실패", e);
        }
    }

    private void logSecurityException(HttpServletRequest request, String requestId,
                                      Exception e, String clientIp) {
        try {
            SecurityAuditEvent event = SecurityAuditEvent.builder()
                    .timestamp(LocalDateTime.now().format(DATE_FORMATTER))
                    .requestId(requestId)
                    .eventType("SECURITY_EXCEPTION")
                    .clientIp(clientIp)
                    .method(request.getMethod())
                    .uri(request.getRequestURI())
                    .exceptionType(e.getClass().getSimpleName())
                    .exceptionMessage(e.getMessage())
                    .build();

            auditLogger.error("SECURITY_AUDIT: {}", new ObjectMapper().writeValueAsString(event));

        } catch (Exception ex) {
            logger.error("보안 예외 로깅 실패", ex);
        }
    }

    private void analyzeIpPattern(String clientIp, HttpServletRequest request) {
        IpRequestPattern pattern = ipPatterns.computeIfAbsent(clientIp, k -> new IpRequestPattern());

        pattern.incrementRequestCount();
        pattern.addRequestPath(request.getRequestURI());
        pattern.addUserAgent(request.getHeader("User-Agent"));

        if (System.currentTimeMillis() - pattern.getLastUpdateTime() > 3600000) {
            ipPatterns.remove(clientIp);
        }
    }

    private void detectSuspiciousPatterns(String clientIp, HttpServletRequest request,
                                          HttpServletResponse response, long duration) {
        IpRequestPattern pattern = ipPatterns.get(clientIp);
        if (pattern == null) return;

        boolean isSuspicious = false;
        String reason = "";

        if (pattern.getRequestCount() > SUSPICIOUS_REQUEST_THRESHOLD) {
            isSuspicious = true;
            reason = "High request frequency";
        }

        if (response.getStatus() >= 400 && pattern.getErrorCount() > SUSPICIOUS_ERROR_THRESHOLD) {
            isSuspicious = true;
            reason = "High error rate";
        }

        if (duration < 50 && pattern.getRequestCount() > 10) {
            isSuspicious = true;
            reason = "Suspiciously fast requests";
        }

        if (isSuspicious) {
            logger.warn("의심스러운 IP 패턴 감지: IP={}, Reason={}, Pattern={}",
                    clientIp, reason, pattern);

            notifySecurityTeam(clientIp, reason, pattern);
        }
    }

    private void notifySecurityTeam(String clientIp, String reason, IpRequestPattern pattern) {

        auditLogger.warn("SECURITY_ALERT: Suspicious IP detected - IP: {}, Reason: {}, Pattern: {}",
                clientIp, reason, pattern);
    }

    private String generateRequestId() {
        return String.format("REQ_%d_%s",
                requestCounter.incrementAndGet(),
                UUID.randomUUID().toString().substring(0, 8));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        return path.startsWith("/static/") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.equals("/health") ||
                path.equals("/actuator/health");
    }

    private static class IpRequestPattern {
        private int requestCount = 0;
        private int errorCount = 0;
        private final java.util.Set<String> requestPaths = new java.util.HashSet<>();
        private final java.util.Set<String> userAgents = new java.util.HashSet<>();
        private long lastUpdateTime = System.currentTimeMillis();

        public void incrementRequestCount() {
            requestCount++;
            lastUpdateTime = System.currentTimeMillis();
        }

        public void incrementErrorCount() {
            errorCount++;
        }

        public void addRequestPath(String path) {
            requestPaths.add(path);
        }

        public void addUserAgent(String userAgent) {
            if (userAgent != null) {
                userAgents.add(userAgent);
            }
        }

        public int getRequestCount() {
            return requestCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public long getLastUpdateTime() {
            return lastUpdateTime;
        }

        @Override
        public String toString() {
            return String.format("Requests: %d, Errors: %d, Paths: %s, UserAgents: %d",
                    requestCount, errorCount, requestPaths.size(), userAgents.size());
        }
    }

    private static class SecurityAuditEvent {
        private String timestamp;
        private String requestId;
        private String eventType;
        private String clientIp;
        private String method;
        private String uri;
        private String queryString;
        private String userAgent;
        private String referer;
        private String contentType;
        private Integer contentLength;
        private Integer statusCode;
        private Long duration;
        private String username;
        private String authorities;
        private String exceptionType;
        private String exceptionMessage;

        public static SecurityAuditEventBuilder builder() {
            return new SecurityAuditEventBuilder();
        }

        public static class SecurityAuditEventBuilder {
            private SecurityAuditEvent event = new SecurityAuditEvent();

            public SecurityAuditEventBuilder timestamp(String timestamp) {
                event.timestamp = timestamp;
                return this;
            }

            public SecurityAuditEventBuilder requestId(String requestId) {
                event.requestId = requestId;
                return this;
            }

            public SecurityAuditEventBuilder eventType(String eventType) {
                event.eventType = eventType;
                return this;
            }

            public SecurityAuditEventBuilder clientIp(String clientIp) {
                event.clientIp = clientIp;
                return this;
            }

            public SecurityAuditEventBuilder method(String method) {
                event.method = method;
                return this;
            }

            public SecurityAuditEventBuilder uri(String uri) {
                event.uri = uri;
                return this;
            }

            public SecurityAuditEventBuilder queryString(String queryString) {
                event.queryString = queryString;
                return this;
            }

            public SecurityAuditEventBuilder userAgent(String userAgent) {
                event.userAgent = userAgent;
                return this;
            }

            public SecurityAuditEventBuilder referer(String referer) {
                event.referer = referer;
                return this;
            }

            public SecurityAuditEventBuilder contentType(String contentType) {
                event.contentType = contentType;
                return this;
            }

            public SecurityAuditEventBuilder contentLength(Integer contentLength) {
                event.contentLength = contentLength;
                return this;
            }

            public SecurityAuditEventBuilder statusCode(Integer statusCode) {
                event.statusCode = statusCode;
                return this;
            }

            public SecurityAuditEventBuilder duration(Long duration) {
                event.duration = duration;
                return this;
            }

            public SecurityAuditEventBuilder username(String username) {
                event.username = username;
                return this;
            }

            public SecurityAuditEventBuilder authorities(String authorities) {
                event.authorities = authorities;
                return this;
            }

            public SecurityAuditEventBuilder exceptionType(String exceptionType) {
                event.exceptionType = exceptionType;
                return this;
            }

            public SecurityAuditEventBuilder exceptionMessage(String exceptionMessage) {
                event.exceptionMessage = exceptionMessage;
                return this;
            }

            public SecurityAuditEvent build() {
                return event;
            }
        }
    }
}