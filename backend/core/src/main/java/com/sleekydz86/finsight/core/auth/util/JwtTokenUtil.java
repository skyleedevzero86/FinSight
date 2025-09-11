package com.sleekydz86.finsight.core.auth.util;

import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenUtil.class);

    @Value("${jwt.secret:defaultSecretKeyForDevelopmentOnly12345678901234567890}")
    private String secret;

    @Value("${jwt.access-token.expiration:3600000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration:2592000000}")
    private long refreshTokenExpiration;

    @Value("${jwt.recovery-token.expiration:300}")
    private long recoveryTokenExpiration;

    @Value("${jwt.issuer:finsight}")
    private String issuer;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] paddedKeyBytes = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKeyBytes, 0, keyBytes.length);
            return Keys.hmacShaKeyFor(paddedKeyBytes);
        }
        return Keys.hmacShaKeyFor(keyBytes);
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
                .claims(claims)
                .subject(email)
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
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
            log.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public String getEmailFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to extract email from token: {}", e.getMessage());
            return null;
        }
    }

    public UserRole getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String roleStr = claims.get("role", String.class);
            return roleStr != null ? UserRole.valueOf(roleStr) : UserRole.USER;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to extract role from token: {}", e.getMessage());
            return UserRole.USER;
        }
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Claims getAllClaimsFromToken(String token) {
        return getClaimsFromToken(token);
    }

    public long getExpirationPeriod() {
        return accessTokenExpiration;
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return "REFRESH".equals(claims.get("tokenType"));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getEmailFromRefreshToken(String token) {
        return getEmailFromToken(token);
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public String getTokenType(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get("tokenType", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public AuthenticatedUser createAuthenticatedUser(String token) {
        try {
            String email = getEmailFromToken(token);
            UserRole role = getRoleFromToken(token);

            if (email == null) {
                return null;
            }

            return AuthenticatedUser.builder()
                    .email(email)
                    .role(role.name())
                    .build();
        } catch (Exception e) {
            log.warn("Failed to create AuthenticatedUser from token: {}", e.getMessage());
            return null;
        }
    }

    public String generateRecoveryToken(String email, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("username", username);
        claims.put("type", "recovery");

        return createToken(email + ":" + username, claims, Duration.ofSeconds(recoveryTokenExpiration));
    }

    private String createToken(String subject, Map<String, Object> claims, Duration expiration) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration.toMillis());

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public String getUserInfoFromRecoveryToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String type = claims.get("type", String.class);

            if (!"recovery".equals(type)) {
                throw new IllegalArgumentException("유효하지 않은 복구 토큰입니다.");
            }

            String email = claims.get("email", String.class);
            String username = claims.get("username", String.class);

            return email + ":" + username;
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 복구 토큰입니다.", e);
        }
    }

    public boolean validateRecoveryToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String type = claims.get("type", String.class);
            return "recovery".equals(type) && !isTokenExpired(claims);
        } catch (Exception e) {
            return false;
        }
    }

    public String generateApiToken(String email, String apiKey) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "API");
        claims.put("apiKey", apiKey);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuer(issuer)
                .issuedAt(new Date())
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public boolean validateApiToken(String token, String expectedApiKey) {
        try {
            Claims claims = getClaimsFromToken(token);
            String type = claims.get("type", String.class);
            String apiKey = claims.get("apiKey", String.class);

            return "API".equals(type) && expectedApiKey.equals(apiKey);
        } catch (Exception e) {
            return false;
        }
    }

    public Date getExpirationDateFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    public long getRemainingTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            if (expiration == null) {
                return 0;
            }
            long remainingTime = expiration.getTime() - System.currentTimeMillis();
            return remainingTime > 0 ? remainingTime : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}