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

    public static JwtTokenBuilder builder() {
        return new JwtTokenBuilder();
    }

    public static class JwtTokenBuilder {
        private String accessToken;
        private String refreshToken;
        private LocalDateTime expiresAt;
        private String tokenType = "Bearer";

        public JwtTokenBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public JwtTokenBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public JwtTokenBuilder expiresIn(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public JwtTokenBuilder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public JwtToken build() {
            return new JwtToken(accessToken, refreshToken, expiresAt);
        }
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