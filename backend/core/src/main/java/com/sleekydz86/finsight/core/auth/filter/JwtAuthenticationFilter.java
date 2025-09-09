
package com.sleekydz86.finsight.core.auth.filter;

import com.sleekydz86.finsight.core.auth.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil, UserDetailsService userDetailsService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        try {
            String token = extractTokenFromRequest(request);

            if (StringUtils.hasText(token)) {
                if (jwtTokenUtil.validateToken(token)) {
                    String tokenType = jwtTokenUtil.getTokenType(token);

                    if ("ACCESS".equals(tokenType)) {
                        Authentication authentication = getAuthentication(token);

                        if (authentication != null && authentication.isAuthenticated()) {
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.debug("인증 설정 성공 - 사용자: {}", authentication.getName());
                        } else {
                            log.warn("토큰에서 인증 생성 실패");
                        }
                    }
                } else {
                    log.warn("JWT 토큰 검증 실패 - 요청: {}", requestURI);
                }
            } else {
                log.debug("JWT 토큰 없음 - 요청: {}", requestURI);
            }
        } catch (Exception e) {
            log.error("JWT 토큰 처리 오류 - 요청 {}: {}", requestURI, e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<Cookie> tokenCookie = Arrays.stream(cookies)
                    .filter(cookie -> "accessToken".equals(cookie.getName()))
                    .findFirst();

            if (tokenCookie.isPresent()) {
                String token = tokenCookie.get().getValue();
                if (StringUtils.hasText(token)) {
                    return token;
                }
            }
        }

        return null;
    }

    private Authentication getAuthentication(String token) {
        try {
            String email = jwtTokenUtil.getEmailFromToken(token);
            if (email != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                return new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
            }
            return null;
        } catch (Exception e) {
            log.error("토큰에서 인증 생성 실패: {}", e.getMessage());
            return null;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        return
                path.equals("/api/v1/auth/login") ||
                path.equals("/api/v1/auth/register") ||
                path.startsWith("/actuator/") ||
                path.equals("/favicon.ico") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/");
    }
}