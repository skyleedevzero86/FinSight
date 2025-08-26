package com.sleekydz86.finsight.core.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        setSecurityHeaders(response);

        filterChain.doFilter(request, response);
    }

    private void setSecurityHeaders(HttpServletResponse response) {
        response.setHeader("X-XSS-Protection", "1; mode=block");

        response.setHeader("X-Frame-Options", "DENY");

        response.setHeader("X-Content-Type-Options", "nosniff");

        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        response.setHeader("Permissions-Policy",
                "geolocation=(), microphone=(), camera=(), payment=(), usb=(), magnetometer=(), gyroscope=()");

        response.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data: https:; " +
                        "font-src 'self' data:; " +
                        "connect-src 'self' https:; " +
                        "frame-ancestors 'none';");

        response.setHeader("Strict-Transport-Security",
                "max-age=31536000; includeSubDomains; preload");

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI().toLowerCase();
        return path.contains("/actuator/health") ||
                path.contains("/actuator/metrics") ||
                path.contains("/health");
    }
}