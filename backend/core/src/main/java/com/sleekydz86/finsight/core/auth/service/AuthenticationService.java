package com.sleekydz86.finsight.core.auth.service;

import com.sleekydz86.finsight.core.auth.domain.JwtToken;
import com.sleekydz86.finsight.core.auth.util.JwtTokenUtil;
import com.sleekydz86.finsight.core.global.exception.AuthenticationFailedException;
import com.sleekydz86.finsight.core.global.exception.TokenExpiredException;
import com.sleekydz86.finsight.core.global.exception.UserNotFoundException;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserPersistencePort userPersistencePort;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthenticationService(UserPersistencePort userPersistencePort,
                                 PasswordEncoder passwordEncoder,
                                 JwtTokenUtil jwtTokenUtil) {
        this.userPersistencePort = userPersistencePort;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    public JwtToken authenticate(String email, String password) {
        log.info("Attempting authentication for user: {}", email);

        try {

            User user = userPersistencePort.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

            if (!passwordEncoder.matches(password, user.getPassword())) {
                log.warn("Password mismatch for user: {}", email);
                throw new AuthenticationFailedException("Invalid password");
            }

            if (!user.isActive()) {
                log.warn("Inactive user attempt to login: {}", email);
                throw new AuthenticationFailedException("User account is deactivated");
            }

            user.updateLastLogin();
            userPersistencePort.save(user);

            String accessToken = jwtTokenUtil.generateAccessToken(email, user.getRole());
            String refreshToken = jwtTokenUtil.generateRefreshToken(email);
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtTokenUtil.getExpirationPeriod() / 1000);

            JwtToken jwtToken = new JwtToken(accessToken, refreshToken, expiresAt);

            log.info("Authentication successful for user: {}", email);
            return jwtToken;

        } catch (UserNotFoundException | AuthenticationFailedException e) {
            log.error("Authentication failed for user: {}", email, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during authentication for user: {}", email, e);
            throw new AuthenticationFailedException("Authentication failed due to unexpected error");
        }
    }

    public JwtToken refreshToken(String refreshToken) {
        log.info("Attempting token refresh");

        try {

            if (!jwtTokenUtil.validateToken(refreshToken)) {
                log.warn("Invalid refresh token provided");
                throw new AuthenticationFailedException("Invalid refresh token");
            }

            if (jwtTokenUtil.isTokenExpired(refreshToken)) {
                log.warn("Expired refresh token provided");
                throw new TokenExpiredException("refresh");
            }

            String email = jwtTokenUtil.getEmailFromToken(refreshToken);

            User user = userPersistencePort.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

            String newAccessToken = jwtTokenUtil.generateAccessToken(email, user.getRole());
            String newRefreshToken = jwtTokenUtil.generateRefreshToken(email);
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtTokenUtil.getExpirationPeriod() / 1000);

            log.info("Token refresh successful for user: {}", email);
            return new JwtToken(newAccessToken, newRefreshToken, expiresAt);

        } catch (UserNotFoundException | AuthenticationFailedException | TokenExpiredException e) {
            log.error("Token refresh failed", e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during token refresh", e);
            throw new AuthenticationFailedException("Token refresh failed due to unexpected error");
        }
    }
}