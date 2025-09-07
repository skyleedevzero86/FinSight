package com.sleekydz86.finsight.core.auth.filter;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    private final Bucket generalBucket;
    private final Bucket authBucket;
    private final Bucket apiBucket;

    public RateLimitFilter() {
        this.generalBucket = null;
        this.authBucket = null;
        this.apiBucket = null;
    }

    public RateLimitFilter(Bucket generalBucket, Bucket authBucket, Bucket apiBucket) {
        this.generalBucket = generalBucket;
        this.authBucket = authBucket;
        this.apiBucket = apiBucket;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (generalBucket == null || authBucket == null || apiBucket == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        Bucket bucket = selectBucket(path);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            logger.warn("Rate limit exceeded for path: {}", path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
        }
    }

    private Bucket selectBucket(String path) {
        if (path.startsWith("/api/auth/")) {
            return authBucket;
        } else if (path.startsWith("/api/")) {
            return apiBucket;
        } else {
            return generalBucket;
        }
    }
}