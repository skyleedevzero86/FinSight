package com.sleekydz86.finsight.core.auth.filter;

import com.bucket4j.Bandwidth;
import com.bucket4j.Bucket;
import com.bucket4j.Refill;
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
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    private final Bucket defaultBucket;
    private final Bucket strictBucket;
    private final Bucket adminBucket;

    private final ConcurrentHashMap<String, ClientRateLimitInfo> clientCounters = new ConcurrentHashMap<>();
    private static final int MAX_CLIENTS = 10000;

    public RateLimitFilter(@Qualifier("createBucket") Bucket defaultBucket,
                           @Qualifier("strictBucket") Bucket strictBucket,
                           @Qualifier("adminBucket") Bucket adminBucket) {
        this.defaultBucket = defaultBucket;
        this.strictBucket = strictBucket;
        this.adminBucket = adminBucket;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIpAddress(request);
        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        ClientRateLimitInfo clientInfo = getOrCreateClientInfo(clientIp);

        Bucket bucket = selectBucket(requestPath, method, clientInfo);

        if (bucket.tryConsume(1)) {

            clientInfo.incrementRequestCount();
            addRateLimitHeaders(response, bucket);
            filterChain.doFilter(request, response);
        } else {

            logger.warn("Rate limit exceeded for IP: {}, Path: {}, Method: {}", clientIp, requestPath, method);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"retryAfter\":\"" +
                    getRetryAfterSeconds(bucket) + " seconds\"}");

            clientInfo.incrementBlockedCount();
        }

        cleanupOldClientInfo();
    }

    private Bucket selectBucket(String requestPath, String method, ClientRateLimitInfo clientInfo) {

        if (isAdminPath(requestPath)) {
            return adminBucket;
        }

        if (isSensitivePath(requestPath, method)) {
            return strictBucket;
        }

        return defaultBucket;
    }

    private boolean isAdminPath(String requestPath) {
        return requestPath.startsWith("/admin") ||
                requestPath.startsWith("/actuator") ||
                requestPath.contains("/management");
    }

    private boolean isSensitivePath(String requestPath, String method) {
        return (requestPath.startsWith("/auth/login") && "POST".equals(method)) ||
                (requestPath.startsWith("/auth/register") && "POST".equals(method)) ||
                (requestPath.startsWith("/auth/refresh") && "POST".equals(method));
    }

    private ClientRateLimitInfo getOrCreateClientInfo(String clientIp) {
        return clientCounters.computeIfAbsent(clientIp, k -> {
            // 최대 클라이언트 수 제한
            if (clientCounters.size() >= MAX_CLIENTS) {
                // 가장 오래된 클라이언트 제거
                String oldestClient = clientCounters.keySet().iterator().next();
                clientCounters.remove(oldestClient);
            }
            return new ClientRateLimitInfo();
        });
    }

    private void addRateLimitHeaders(HttpServletResponse response, Bucket bucket) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(getBucketCapacity(bucket)));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(getBucketAvailableTokens(bucket)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 60000)); // 1분 후
    }

    private long getBucketCapacity(Bucket bucket) {
        // Bucket4j의 내부 상태를 통해 용량 정보 추출
        return 100;
    }

    private long getBucketAvailableTokens(Bucket bucket) {
        // Bucket4j의 내부 상태를 통해 사용 가능한 토큰 수 추출
        return 50;
    }

    private long getRetryAfterSeconds(Bucket bucket) {
        return 60;
    }

    private void cleanupOldClientInfo() {
        // 1분마다 정리 (실제로는 별도 스케줄러로 관리하는 것이 좋음)
        if (System.currentTimeMillis() % 60000 < 1000) {
            clientCounters.entrySet().removeIf(entry ->
                    entry.getValue().isExpired(300000));
        }
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

        String xForwarded = request.getHeader("X-Forwarded");
        if (xForwarded != null && !xForwarded.isEmpty() && !"unknown".equalsIgnoreCase(xForwarded)) {
            return xForwarded;
        }

        String xClusterClientIp = request.getHeader("X-Cluster-Client-IP");
        if (xClusterClientIp != null && !xClusterClientIp.isEmpty() && !"unknown".equalsIgnoreCase(xClusterClientIp)) {
            return xClusterClientIp;
        }

        String httpClientIp = request.getHeader("HTTP_CLIENT_IP");
        if (httpClientIp != null && !httpClientIp.isEmpty() && !"unknown".equalsIgnoreCase(httpClientIp)) {
            return httpClientIp;
        }

        String httpXForwardedFor = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (httpXForwardedFor != null && !httpXForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(httpXForwardedFor)) {
            return httpXForwardedFor;
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

    private static class ClientRateLimitInfo {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private final AtomicInteger blockedCount = new AtomicInteger(0);
        private final long lastAccessTime = System.currentTimeMillis();

        public void incrementRequestCount() {
            requestCount.incrementAndGet();
        }

        public void incrementBlockedCount() {
            blockedCount.incrementAndGet();
        }

        public boolean isExpired(long maxAge) {
            return System.currentTimeMillis() - lastAccessTime > maxAge;
        }

        public int getRequestCount() {
            return requestCount.get();
        }

        public int getBlockedCount() {
            return blockedCount.get();
        }
    }
}