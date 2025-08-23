package com.sleekydz86.finsight.core.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class SecurityAuditFilter extends OncePerRequestFilter {

    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString().substring(0, 8);

        // MDC에 요청 정보 추가 (로그 추적용)
        MDC.put("requestId", requestId);
        MDC.put("clientIp", getClientIpAddress(request));
        MDC.put("userAgent", request.getHeader("User-Agent"));

        long startTime = System.currentTimeMillis();

        try {
            logRequestStart(request, requestId);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logSecurityException(request, requestId, e);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequestEnd(request, response, requestId, duration);

            MDC.clear();
        }
    }

    private void logRequestStart(HttpServletRequest request, String requestId) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");

        auditLogger.info("REQUEST_START - ID: {}, Method: {}, URI: {}, Query: {}, IP: {}, UserAgent: {}, Referer: {}",
                requestId, method, uri, queryString, clientIp,
                StringUtils.hasText(userAgent) ? userAgent.substring(0, Math.min(userAgent.length(), 100)) : "N/A",
                referer);
    }

    private void logRequestEnd(HttpServletRequest request, HttpServletResponse response, String requestId, long duration) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = getUsername(authentication);
        String authorities = getAuthorities(authentication);
        int status = response.getStatus();

        auditLogger.info("REQUEST_END - ID: {}, Status: {}, Duration: {}ms, User: {}, Roles: {}",
                requestId, status, duration, username, authorities);

        if (isSecuritySensitiveRequest(request)) {
            auditLogger.warn("SECURITY_SENSITIVE_REQUEST - ID: {}, URI: {}, User: {}, Status: {}",
                    requestId, request.getRequestURI(), username, status);
        }

        if (status == 401 || status == 403) {
            auditLogger.warn("ACCESS_DENIED - ID: {}, URI: {}, User: {}, Status: {}, IP: {}",
                    requestId, request.getRequestURI(), username, status, getClientIpAddress(request));
        }
    }

    private void logSecurityException(HttpServletRequest request, String requestId, Exception e) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = getUsername(authentication);

        auditLogger.error("SECURITY_EXCEPTION - ID: {}, URI: {}, User: {}, IP: {}, Exception: {}",
                requestId, request.getRequestURI(), username, getClientIpAddress(request),
                e.getMessage(), e);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "X-Original-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    private String getUsername(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {
            return authentication.getName();
        }
        return "anonymous";
    }

    private String getAuthorities(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().toString();
        }
        return "[]";
    }

    private boolean isSecuritySensitiveRequest(HttpServletRequest request) {
        String uri = request.getRequestURI().toLowerCase();
        String method = request.getMethod();

        return uri.contains("/admin") ||
                uri.contains("/auth") ||
                uri.contains("/user") ||
                uri.contains("/security") ||
                "DELETE".equals(method) ||
                "PUT".equals(method) && uri.contains("/sensitive");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI().toLowerCase();
        return path.contains("/css/") ||
                path.contains("/js/") ||
                path.contains("/images/") ||
                path.contains("/fonts/") ||
                path.contains("/favicon.ico") ||
                path.contains("/actuator/health") ||
                path.contains("/actuator/metrics");
    }
}