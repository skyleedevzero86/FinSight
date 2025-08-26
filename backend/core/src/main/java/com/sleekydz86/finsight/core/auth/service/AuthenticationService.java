package com.sleekydz86.finsight.core.auth.service;

import com.sleekydz86.finsight.core.auth.domain.JwtToken;
import com.sleekydz86.finsight.core.auth.util.JwtTokenUtil;
import com.sleekydz86.finsight.core.global.exception.AuthenticationFailedException;
import com.sleekydz86.finsight.core.global.exception.InvalidTokenException;
import com.sleekydz86.finsight.core.global.exception.TokenExpiredException;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

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
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수입니다");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다");
        }

        Optional<User> userOpt = userPersistencePort.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new AuthenticationFailedException(email);
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthenticationFailedException(email);
        }

        if (!user.isActive()) {
            throw new AuthenticationFailedException(email);
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


        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new InvalidTokenException("refresh");
        }

        try {
            if (!jwtTokenUtil.validateToken(refreshToken)) {
                throw new InvalidTokenException("refresh");
            }

            if (jwtTokenUtil.isTokenExpired(refreshToken)) {
                throw new TokenExpiredException("refresh");
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