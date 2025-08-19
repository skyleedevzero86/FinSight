package com.sleekydz86.finsight.core.auth.service;

import com.sleekydz86.finsight.core.auth.domain.JwtToken;
import com.sleekydz86.finsight.core.auth.util.JwtTokenUtil;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@Transactional
public class AuthenticationService {

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
        User user = userPersistencePort.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new IllegalStateException("User account is deactivated");
        }

        user.updateLastLogin();
        userPersistencePort.save(user);

        String accessToken = jwtTokenUtil.generateAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtTokenUtil.generateRefreshToken(user.getEmail());
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                new Date(System.currentTimeMillis() + 3600000).toInstant(),
                ZoneId.systemDefault());

        return new JwtToken(accessToken, refreshToken, expiresAt);
    }

    public JwtToken refreshToken(String refreshToken) {
        if (!jwtTokenUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String email = jwtTokenUtil.getEmailFromToken(refreshToken);
        User user = userPersistencePort.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String newAccessToken = jwtTokenUtil.generateAccessToken(user.getEmail(), user.getRole());
        String newRefreshToken = jwtTokenUtil.generateRefreshToken(user.getEmail());
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                new Date(System.currentTimeMillis() + 3600000).toInstant(),
                ZoneId.systemDefault());

        return new JwtToken(newAccessToken, newRefreshToken, expiresAt);
    }
}