package com.sleekydz86.finsight.core.auth.util;

import com.sleekydz86.finsight.core.global.exception.AuthenticationFailedException;
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
import java.util.UUID;

@Component
public class JwtTokenUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-period}")
    private long expirationPeriod;

    @Value("${jwt.refresh-expiration-period:86400000}")
    private long refreshExpirationPeriod;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String email, UserRole role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationPeriod);

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role.name())
                .claim("type", "ACCESS")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setId(generateJti())
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationPeriod);

        return Jwts.builder()
                .setSubject(email)
                .claim("type", "REFRESH")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setId(generateJti())
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    private String generateJti() {
        return UUID.randomUUID().toString();
    }

    public String getEmailFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public UserRole getRoleFromToken(String token) {
        String roleStr = getClaimFromToken(token, claims -> claims.get("role", String.class));
        return roleStr != null ? UserRole.valueOf(roleStr) : UserRole.USER;
    }

    public String getTokenType(String token) {
        return getClaimFromToken(token, claims -> claims.get("type", String.class));
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);

            String tokenType = claims.get("type", String.class);
            if (tokenType == null) {
                log.warn("Token type is missing");
                return false;
            }

            if (isTokenExpired(token)) {
                log.warn("Token is expired");
                return false;
            }

            String jti = claims.getId();
            if (jti == null || jti.isEmpty()) {
                log.warn("Token JTI is missing");
                return false;
            }

            log.debug("Token validation successful for type: {}", tokenType);
            return true;

        } catch (Exception e) {
            log.error("Token validation failed", e);
            return false;
        }
    }

    private <T> T getClaimFromToken(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        try {

            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Failed to parse JWT token", e);
            throw new AuthenticationFailedException("Invalid token format");
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            log.error("Failed to check token expiration", e);
            return true;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            String tokenType = getTokenType(token);
            return "REFRESH".equals(tokenType);
        } catch (Exception e) {
            log.error("Failed to check token type", e);
            return false;
        }
    }

    public long getExpirationPeriod() {
        return expirationPeriod;
    }

    public long getRefreshExpirationPeriod() {
        return refreshExpirationPeriod;
    }
}