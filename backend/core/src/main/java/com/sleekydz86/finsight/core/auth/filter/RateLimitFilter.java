package com.sleekydz86.finsight.core.auth.filter;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    private final Bucket bucket;

    public RateLimitFilter(@Qualifier("createBucket") Bucket bucket) {
        this.bucket = bucket;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIpAddress(request);

        if (bucket.tryConsume(1)) {
            logger.debug("Rate limit passed for IP: {}, URI: {}", clientIp, request.getRequestURI());
            filterChain.doFilter(request, response);
        } else {
            logger.warn("Rate limit exceeded for IP: {}, URI: {}", clientIp, request.getRequestURI());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            String jsonResponse = """
                {
                    "error": "Rate limit exceeded",
                    "message": "Too many requests. Please try again later.",
                    "status": 429,
                    "timestamp": "%s"
                }
                """.formatted(java.time.LocalDateTime.now().toString());

            response.getWriter().write(jsonResponse);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.contains(",") ? ip.split(",")[0].trim() : ip;
            }
        }

        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI().toLowerCase();
        return path.contains("/css/") ||
                path.contains("/js/") ||
                path.contains("/images/") ||
                path.contains("/actuator/health") ||
                path.contains("/actuator/metrics");
    }
}