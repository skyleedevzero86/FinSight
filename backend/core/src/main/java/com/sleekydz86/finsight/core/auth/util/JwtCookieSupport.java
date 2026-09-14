package com.sleekydz86.finsight.core.auth.util;

import com.sleekydz86.finsight.core.auth.domain.JwtToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class JwtCookieSupport {

    public static final String ACCESS_COOKIE = "accessToken";
    public static final String REFRESH_COOKIE = "refreshToken";
    public static final String AUTH_HINT_COOKIE = "finsight_auth";

    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    private final boolean secureCookies;

    public JwtCookieSupport(
            @Value("${jwt.access-token.expiration:3600000}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token.expiration:2592000000}") long refreshTokenExpirationMs,
            @Value("${app.auth.cookie.secure:false}") boolean secureCookies) {
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.secureCookies = secureCookies;
    }

    public void writeAuthCookies(HttpHeaders headers, JwtToken token) {
        if (token == null) {
            return;
        }
        if (token.getAccessToken() != null && !token.getAccessToken().isBlank()) {
            headers.add(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, token.getAccessToken(), true,
                    Duration.ofMillis(Math.max(1_000L, accessTokenExpirationMs))).toString());
            headers.add(HttpHeaders.SET_COOKIE, cookie(AUTH_HINT_COOKIE, "1", false,
                    Duration.ofMillis(Math.max(1_000L, accessTokenExpirationMs))).toString());
        }
        if (token.getRefreshToken() != null && !token.getRefreshToken().isBlank()) {
            headers.add(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, token.getRefreshToken(), true,
                    Duration.ofMillis(Math.max(1_000L, refreshTokenExpirationMs))).toString());
        }
    }

    public void clearAuthCookies(HttpHeaders headers) {
        headers.add(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, "", true, Duration.ZERO).toString());
        headers.add(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, "", true, Duration.ZERO).toString());
        headers.add(HttpHeaders.SET_COOKIE, cookie(AUTH_HINT_COOKIE, "", false, Duration.ZERO).toString());
    }

    private ResponseCookie cookie(String name, String value, boolean httpOnly, Duration maxAge) {
        return ResponseCookie.from(name, value == null ? "" : value)
                .path("/")
                .sameSite("Lax")
                .secure(secureCookies)
                .httpOnly(httpOnly)
                .maxAge(maxAge)
                .build();
    }
}
