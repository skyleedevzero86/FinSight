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
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    public String getEmailFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to extract email from token: {}", e.getMessage());
            return null;
        }
    }

    public UserRole getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String roleStr = claims.get("role", String.class);
            return roleStr != null ? UserRole.valueOf(roleStr) : UserRole.USER;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to extract role from token: {}", e.getMessage());
            return UserRole.USER;
        }
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
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

}