package com.sleekydz86.finsight.core.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.sleekydz86.finsight.core.user.domain.UserRole;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenUtil.class);

    @Value("${jwt.secret:defaultSecretKeyForDevelopmentOnly}")
    private String secret;

    @Value("${jwt.access-token.expiration:3600000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration:2592000000}")
    private long refreshTokenExpiration;

    @Value("${jwt.issuer:finsight}")
    private String issuer;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(String email, UserRole role) {
        return generateToken(email, "ACCESS", role, accessTokenExpiration);
    }

    public String generateRefreshToken(String email) {
        return generateToken(email, "REFRESH", null, refreshTokenExpiration);
    }

    private String generateToken(String email, String tokenType, UserRole role, long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", tokenType);
        if (role != null) {
            claims.put("role", role.name());
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuer(issuer)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.warn("Token expiration check failed: {}", e.getMessage());
            return true;
        }
    }

    public String getEmailFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.warn("Failed to extract email from token: {}", e.getMessage());
            return null;
        }
    }

    public UserRole getRoleFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String roleStr = claims.get("role", String.class);
            return roleStr != null ? UserRole.valueOf(roleStr) : null;
        } catch (Exception e) {
            log.warn("Failed to extract role from token: {}", e.getMessage());
            return null;
        }
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationPeriod() {
        return accessTokenExpiration;
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String tokenType = claims.get("tokenType", String.class);
            return "REFRESH".equals(tokenType) && !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("리프레시 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public String getEmailFromRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.warn("리프레시 토큰에서 이메일 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
}