package com.sleekydz86.finsight.core.auth.service;

import com.sleekydz86.finsight.core.auth.dto.AccountRecoveryRequest;
import com.sleekydz86.finsight.core.auth.dto.AccountRecoveryResponse;
import com.sleekydz86.finsight.core.auth.dto.AccountRecoveryVerifyRequest;
import com.sleekydz86.finsight.core.auth.dto.PasswordResetRequest;
import com.sleekydz86.finsight.core.auth.util.JwtTokenUtil;
import com.sleekydz86.finsight.core.global.exception.UserNotFoundException;
import com.sleekydz86.finsight.core.notification.service.EmailNotificationService;
import com.sleekydz86.finsight.core.notification.service.SmsNotificationService;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private final UserService userService;
    private final OtpService otpService;
    private final EmailNotificationService emailNotificationService;
    private final SmsNotificationService smsNotificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.recovery.token.expiration:1800}")
    private long recoveryTokenExpiration;

    @Value("${app.recovery.otp.expiration:300}")
    private long otpExpiration;

    private static final String RECOVERY_TOKEN_PREFIX = "recovery:token:";
    private static final String RECOVERY_OTP_PREFIX = "recovery:otp:";

    @Transactional
    public AccountRecoveryResponse initiateAccountRecovery(AccountRecoveryRequest request) {
        try {
            User user = userService.findByEmailAndUsername(request.getEmail(), request.getUsername())
                    .orElseThrow(() -> new UserNotFoundException("해당 이메일과 사용자명으로 등록된 계정을 찾을 수 없습니다."));

            String otpCode = otpService.generateOtp();
            String otpKey = RECOVERY_OTP_PREFIX + user.getEmail() + ":" + user.getUsername();

            redisTemplate.opsForValue().set(otpKey, otpCode, otpExpiration, TimeUnit.SECONDS);

            sendRecoveryOtp(user, otpCode);

            log.info("계정 복구 OTP 발송 완료 - 사용자: {}, 이메일: {}", user.getUsername(), user.getEmail());

            return AccountRecoveryResponse.builder()
                    .success(true)
                    .message("계정 복구를 위한 OTP가 발송되었습니다. 이메일과 SMS를 확인해주세요.")
                    .build();

        } catch (Exception e) {
            log.error("계정 복구 초기화 실패 - 이메일: {}, 사용자명: {}, 오류: {}",
                    request.getEmail(), request.getUsername(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public AccountRecoveryResponse verifyRecoveryOtp(AccountRecoveryVerifyRequest request) {
        try {
            User user = userService.findByEmailAndUsername(request.getEmail(), request.getUsername())
                    .orElseThrow(() -> new UserNotFoundException("해당 이메일과 사용자명으로 등록된 계정을 찾을 수 없습니다."));

            String otpKey = RECOVERY_OTP_PREFIX + user.getEmail() + ":" + user.getUsername();
            String storedOtp = redisTemplate.opsForValue().get(otpKey);

            if (storedOtp == null) {
                throw new IllegalArgumentException("OTP가 만료되었거나 존재하지 않습니다.");
            }

            if (!storedOtp.equals(request.getOtpCode())) {
                throw new IllegalArgumentException("OTP 코드가 일치하지 않습니다.");
            }

            String recoveryToken = jwtTokenUtil.generateRecoveryToken(user.getEmail(), user.getUsername());
            String tokenKey = RECOVERY_TOKEN_PREFIX + user.getEmail() + ":" + user.getUsername();

            redisTemplate.opsForValue().set(tokenKey, recoveryToken, recoveryTokenExpiration, TimeUnit.SECONDS);
            redisTemplate.delete(otpKey);

            log.info("계정 복구 OTP 검증 성공 - 사용자: {}, 이메일: {}", user.getUsername(), user.getEmail());

            return AccountRecoveryResponse.builder()
                    .success(true)
                    .message("OTP 검증이 완료되었습니다. 비밀번호를 재설정할 수 있습니다.")
                    .recoveryToken(recoveryToken)
                    .expiresIn(recoveryTokenExpiration)
                    .build();

        } catch (Exception e) {
            log.error("계정 복구 OTP 검증 실패 - 이메일: {}, 사용자명: {}, 오류: {}",
                    request.getEmail(), request.getUsername(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public AccountRecoveryResponse resetPassword(PasswordResetRequest request) {
        try {
            String userInfo = jwtTokenUtil.getUserInfoFromRecoveryToken(request.getRecoveryToken());
            String[] parts = userInfo.split(":");
            String email = parts[0];
            String username = parts[1];

            User user = userService.findByEmailAndUsername(email, username)
                    .orElseThrow(() -> new UserNotFoundException("해당 계정을 찾을 수 없습니다."));

            String tokenKey = RECOVERY_TOKEN_PREFIX + email + ":" + username;
            String storedToken = redisTemplate.opsForValue().get(tokenKey);

            if (storedToken == null || !storedToken.equals(request.getRecoveryToken())) {
                throw new IllegalArgumentException("유효하지 않은 복구 토큰입니다.");
            }

            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new IllegalArgumentException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
            }

            userService.updatePassword(user.getId(), request.getNewPassword());
            redisTemplate.delete(tokenKey);

            sendPasswordResetConfirmation(user);

            log.info("비밀번호 재설정 완료 - 사용자: {}, 이메일: {}", user.getUsername(), user.getEmail());

            return AccountRecoveryResponse.builder()
                    .success(true)
                    .message("비밀번호가 성공적으로 재설정되었습니다.")
                    .build();

        } catch (Exception e) {
            log.error("비밀번호 재설정 실패 - 오류: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void sendRecoveryOtp(User user, String otpCode) {
        try {
            String emailSubject = "[FinSight] 계정 복구를 위한 OTP 코드";
            String emailContent = createRecoveryOtpEmailContent(user, otpCode);

            emailNotificationService.sendRecoveryOtpEmail(user, emailSubject, emailContent);

            if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                String smsContent = String.format("[FinSight] 계정 복구 OTP: %s (5분간 유효)", otpCode);
                smsNotificationService.sendSms(user.getPhoneNumber(), smsContent);
            }

        } catch (Exception e) {
            log.error("복구 OTP 발송 실패 - 사용자: {}, 오류: {}", user.getUsername(), e.getMessage(), e);
        }
    }

    private void sendPasswordResetConfirmation(User user) {
        try {
            String emailSubject = "[FinSight] 비밀번호 재설정 완료 안내";
            String emailContent = createPasswordResetConfirmationEmailContent(user);

            emailNotificationService.sendPasswordResetConfirmationEmail(user, emailSubject, emailContent);

            if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                String smsContent = "[FinSight] 비밀번호가 성공적으로 재설정되었습니다. 보안을 위해 로그인 후 비밀번호를 변경해주세요.";
                smsNotificationService.sendSms(user.getPhoneNumber(), smsContent);
            }

        } catch (Exception e) {
            log.error("비밀번호 재설정 확인 발송 실패 - 사용자: {}, 오류: {}", user.getUsername(), e.getMessage(), e);
        }
    }

    private String createRecoveryOtpEmailContent(User user, String otpCode) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>계정 복구 OTP</title>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #dc2626; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background: #f8f9fa; }
                        .otp-code { background: #e5e7eb; padding: 20px; text-align: center; font-size: 24px; font-weight: bold; margin: 20px 0; }
                        .footer { padding: 10px; text-align: center; font-size: 12px; color: #666; }
                        .warning { background: #fef3c7; border: 1px solid #f59e0b; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>계정 복구 안내</h1>
                        </div>
                        <div class="content">
                            <h2>안녕하세요, %s님!</h2>
                            <p>계정 복구를 요청하셨습니다. 아래 OTP 코드를 입력하여 계정 복구를 진행해주세요.</p>
                            <div class="otp-code">%s</div>
                            <div class="warning">
                                <strong>주의사항:</strong>
                                <ul>
                                    <li>이 OTP 코드는 5분간만 유효합니다.</li>
                                    <li>본인이 요청하지 않은 경우, 이 이메일을 무시하세요.</li>
                                    <li>OTP 코드를 다른 사람과 공유하지 마세요.</li>
                                </ul>
                            </div>
                        </div>
                        <div class="footer">
                            <p>© 2024 FinSight. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, user.getUsername(), otpCode);
    }

    private String createPasswordResetConfirmationEmailContent(User user) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>비밀번호 재설정 완료</title>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #059669; color: white; padding: 20px; text-align: center; }
                        .content { padding: 20px; background: #f8f9fa; }
                        .footer { padding: 10px; text-align: center; font-size: 12px; color: #666; }
                        .success { background: #d1fae5; border: 1px solid #10b981; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>비밀번호 재설정 완료</h1>
                        </div>
                        <div class="content">
                            <h2>안녕하세요, %s님!</h2>
                            <div class="success">
                                <strong>비밀번호가 성공적으로 재설정되었습니다.</strong>
                            </div>
                            <p>보안을 위해 다음 사항을 확인해주세요:</p>
                            <ul>
                                <li>새로운 비밀번호로 로그인해보세요.</li>
                                <li>정기적으로 비밀번호를 변경하세요.</li>
                                <li>의심스러운 활동이 발견되면 즉시 고객지원에 연락하세요.</li>
                            </ul>
                        </div>
                        <div class="footer">
                            <p>© 2024 FinSight. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, user.getUsername());
    }
}