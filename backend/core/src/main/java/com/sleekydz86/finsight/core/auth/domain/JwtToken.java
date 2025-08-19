package com.sleekydz86.finsight.core.auth.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class JwtToken {
    private final String accessToken;
    private final String refreshToken;
    private final LocalDateTime expiresAt;
    private final String tokenType;

    public JwtToken(String accessToken, String refreshToken, LocalDateTime expiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.tokenType = "Bearer";
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getTokenType() {
        return tokenType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        JwtToken jwtToken = (JwtToken) o;
        return Objects.equals(accessToken, jwtToken.accessToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken);
    }
}