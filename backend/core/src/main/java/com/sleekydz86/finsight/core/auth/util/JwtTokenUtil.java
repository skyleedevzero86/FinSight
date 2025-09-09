package com.sleekydz86.finsight.core.auth.util;

import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
        Date now = new Date();
        Date validity = new Date(now.getTime() + expiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", tokenType);
        if (role != null) {
            claims.put("role", role.name());
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return !isTokenExpired(token) && claims.getIssuer().equals(issuer);
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public String getEmailFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Failed to extract email from token: {}", e.getMessage());
            return null;
        }
    }

    public UserRole getRoleFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String roleStr = claims.get("role", String.class);
            return roleStr != null ? UserRole.valueOf(roleStr) : UserRole.USER;
        } catch (Exception e) {
            log.error("Failed to extract role from token: {}", e.getMessage());
            return UserRole.USER;
        }
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
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
            return "REFRESH".equals(claims.get("type", String.class)) && !isTokenExpired(token);
        } catch (Exception e) {
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
            return claims.get("type", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public AuthenticatedUser createAuthenticatedUser(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String email = claims.getSubject();
            UserRole role = UserRole.valueOf(claims.get("role", String.class));

            return AuthenticatedUser.of(null, email, email, role);
        } catch (Exception e) {
            log.error("Failed to create AuthenticatedUser from token: {}", e.getMessage());
            return null;
        }
    }
}