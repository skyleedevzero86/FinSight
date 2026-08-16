package com.sleekydz86.finsight.core.auth.email;

import com.sleekydz86.finsight.core.auth.email.dto.EmailVerificationChallengeResponse;
import com.sleekydz86.finsight.core.auth.email.dto.EmailVerificationConfirmResponse;
import com.sleekydz86.finsight.core.auth.email.dto.EmailVerificationIssueResponse;
import com.sleekydz86.finsight.core.global.exception.EmailVerificationException;
import com.sleekydz86.finsight.core.global.exception.InvalidPasswordException;
import com.sleekydz86.finsight.core.notification.service.EmailNotificationService;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import com.sleekydz86.finsight.core.user.service.PasswordValidationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final int CODE_TTL_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_REQUESTS_PER_HOUR = 8;
    private static final DateTimeFormatter REQUESTED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm").withLocale(Locale.KOREAN);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationJpaRepository repository;
    private final EmailVerificationTokenCodec tokenCodec;
    private final ClientRequestMetaResolver requestMetaResolver;
    private final EmailNotificationService emailNotificationService;
    private final UserPersistencePort userPersistencePort;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidationService passwordValidationService;

    public EmailVerificationService(
            EmailVerificationJpaRepository repository,
            EmailVerificationTokenCodec tokenCodec,
            ClientRequestMetaResolver requestMetaResolver,
            EmailNotificationService emailNotificationService,
            UserPersistencePort userPersistencePort,
            PasswordEncoder passwordEncoder,
            PasswordValidationService passwordValidationService) {
        this.repository = repository;
        this.tokenCodec = tokenCodec;
        this.requestMetaResolver = requestMetaResolver;
        this.emailNotificationService = emailNotificationService;
        this.userPersistencePort = userPersistencePort;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidationService = passwordValidationService;
    }

    @Transactional
    public EmailVerificationIssueResponse issue(String rawEmail, EmailVerificationPurpose purpose, HttpServletRequest request) {
        String email = normalizeEmail(rawEmail);
        validateIssueRules(email, purpose);

        expireStalePending(email, purpose);

        String code = generateCode();
        String challengeId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(CODE_TTL_MINUTES);
        String ip = requestMetaResolver.resolveClientIp(request);
        String location = requestMetaResolver.resolveLocation(ip);
        String requestedAtText = now.atOffset(ZoneOffset.UTC).format(REQUESTED_AT_FORMAT) + " UTC";

        EmailVerificationJpaEntity entity = new EmailVerificationJpaEntity();
        entity.setPurpose(purpose);
        entity.setPurposeLabel(purpose.getLabel());
        entity.setEmail(email);
        entity.setChallengeId(challengeId);
        entity.setCodeHash(passwordEncoder.encode(code));
        entity.setRequestIp(ip);
        entity.setRequestLocation(location);
        entity.setRequestedAt(now);
        entity.setExpiresAt(expiresAt);
        entity.setAttemptCount(0);
        entity.setStatus(EmailVerificationStatus.PENDING);
        repository.save(entity);

        try {
            emailNotificationService.sendVerificationCodeEmail(
                    email,
                    code,
                    purpose.getLabel(),
                    requestedAtText,
                    location);
        } catch (RuntimeException e) {
            repository.delete(entity);
            throw e;
        }

        String token = tokenCodec.encrypt(challengeId);
        log.info("이메일 인증 코드 발송 - purpose={}, email={}", purpose, maskEmail(email));
        return EmailVerificationIssueResponse.builder()
                .challengeToken(token)
                .maskedEmail(maskEmail(email))
                .purpose(purpose)
                .purposeLabel(purpose.getLabel())
                .expiresInSeconds(CODE_TTL_MINUTES * 60)
                .build();
    }

    @Transactional(readOnly = true)
    public EmailVerificationChallengeResponse getChallenge(String token) {
        EmailVerificationJpaEntity entity = requireEntity(token);
        boolean expired = isExpired(entity);
        int remaining = expired ? 0 : (int) Math.max(0, Duration.between(LocalDateTime.now(), entity.getExpiresAt()).getSeconds());
        return EmailVerificationChallengeResponse.builder()
                .maskedEmail(maskEmail(entity.getEmail()))
                .purpose(entity.getPurpose())
                .purposeLabel(entity.getPurposeLabel())
                .status(expired && entity.getStatus() == EmailVerificationStatus.PENDING
                        ? EmailVerificationStatus.EXPIRED
                        : entity.getStatus())
                .expiresInSeconds(remaining)
                .expired(expired)
                .build();
    }

    @Transactional
    public EmailVerificationConfirmResponse confirm(String token, String code) {
        EmailVerificationJpaEntity entity = requireEntity(token);
        if (entity.getStatus() == EmailVerificationStatus.PASSED) {
            return toConfirmResponse(entity, true);
        }
        if (entity.getStatus() == EmailVerificationStatus.FAILED) {
            throw new EmailVerificationException("인증 시도 횟수를 초과했습니다. 다시 인증하기를 눌러 주세요.");
        }
        if (isExpired(entity)) {
            entity.setStatus(EmailVerificationStatus.EXPIRED);
            repository.save(entity);
            throw new EmailVerificationException("인증 코드가 만료되었습니다. 다시 인증하기를 눌러 주세요.");
        }

        String normalizedCode = code == null ? "" : code.trim();
        entity.setAttemptCount(entity.getAttemptCount() + 1);
        entity.setLastEnteredCode(normalizedCode);

        if (!passwordEncoder.matches(normalizedCode, entity.getCodeHash())) {
            if (entity.getAttemptCount() >= MAX_ATTEMPTS) {
                entity.setStatus(EmailVerificationStatus.FAILED);
            }
            repository.save(entity);
            int left = Math.max(0, MAX_ATTEMPTS - entity.getAttemptCount());
            throw new EmailVerificationException(
                    left == 0
                            ? "인증 코드가 올바르지 않습니다. 시도 횟수를 초과했습니다."
                            : "인증 코드가 올바르지 않습니다. 남은 시도 " + left + "회");
        }

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(EmailVerificationStatus.PASSED);
        entity.setVerifiedAt(now);
        entity.setVerifiedCode(normalizedCode);
        repository.save(entity);
        log.info("이메일 인증 통과 - purpose={}, email={}, code={}, at={}",
                entity.getPurpose(), maskEmail(entity.getEmail()), normalizedCode, now);
        return toConfirmResponse(entity, true);
    }

    @Transactional
    public void resetPassword(String token, String username, String rawPassword) {
        EmailVerificationJpaEntity entity = requireEntity(token);
        if (entity.getPurpose() != EmailVerificationPurpose.FIND_PASSWORD) {
            throw new EmailVerificationException("비밀번호 찾기 인증이 아닙니다.");
        }
        if (entity.getStatus() != EmailVerificationStatus.PASSED) {
            throw new EmailVerificationException("이메일 인증을 먼저 완료해 주세요.");
        }
        if (entity.getConsumedAt() != null) {
            throw new EmailVerificationException("이미 사용된 인증입니다. 다시 인증해 주세요.");
        }
        if (entity.getVerifiedAt() != null && entity.getVerifiedAt().plusMinutes(30).isBefore(LocalDateTime.now())) {
            throw new EmailVerificationException("인증 유효 시간이 지났습니다. 다시 인증해 주세요.");
        }

        PasswordValidationService.PasswordValidationResult validation =
                passwordValidationService.validatePassword(rawPassword);
        if (!validation.isValid()) {
            throw new InvalidPasswordException(validation.getErrors());
        }

        User user = userPersistencePort.findByEmail(entity.getEmail())
                .orElseThrow(() -> new EmailVerificationException("가입된 계정을 찾을 수 없습니다."));
        if (username == null || !user.getUsername().equalsIgnoreCase(username.trim())) {
            throw new EmailVerificationException("아이디가 일치하지 않습니다.");
        }
        user.changePassword(passwordEncoder.encode(rawPassword));
        userPersistencePort.save(user);
        entity.setConsumedAt(LocalDateTime.now());
        repository.save(entity);
        log.info("비밀번호 재설정 완료 - email={}, username={}", maskEmail(entity.getEmail()), user.getUsername());
    }

    @Transactional
    public void consumeSignupVerification(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        EmailVerificationJpaEntity entity = requirePassed(email, EmailVerificationPurpose.SIGNUP);
        entity.setConsumedAt(LocalDateTime.now());
        repository.save(entity);
    }

    public void requirePassedSignup(String rawEmail) {
        requirePassed(normalizeEmail(rawEmail), EmailVerificationPurpose.SIGNUP);
    }

    private EmailVerificationJpaEntity requirePassed(String email, EmailVerificationPurpose purpose) {
        EmailVerificationJpaEntity entity = repository
                .findFirstByEmailAndPurposeAndStatusOrderByVerifiedAtDesc(email, purpose, EmailVerificationStatus.PASSED)
                .orElseThrow(() -> new EmailVerificationException("이메일 인증을 먼저 완료해 주세요."));
        if (entity.getConsumedAt() != null) {
            throw new EmailVerificationException("이메일 인증을 다시 진행해 주세요.");
        }
        if (entity.getVerifiedAt() == null || entity.getVerifiedAt().plusHours(24).isBefore(LocalDateTime.now())) {
            throw new EmailVerificationException("이메일 인증이 만료되었습니다. 다시 인증해 주세요.");
        }
        return entity;
    }

    private void validateIssueRules(String email, EmailVerificationPurpose purpose) {
        long recent = repository.countByEmailAndPurposeAndRequestedAtAfter(
                email, purpose, LocalDateTime.now().minusHours(1));
        if (recent >= MAX_REQUESTS_PER_HOUR) {
            throw new EmailVerificationException("인증 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }
        boolean exists = userPersistencePort.existsByEmail(email);
        if (purpose == EmailVerificationPurpose.SIGNUP && exists) {
            throw new EmailVerificationException("이미 가입된 이메일입니다.");
        }
        if ((purpose == EmailVerificationPurpose.FIND_EMAIL || purpose == EmailVerificationPurpose.FIND_PASSWORD)
                && !exists) {
            throw new EmailVerificationException("가입된 계정을 찾을 수 없습니다.");
        }
    }

    private void expireStalePending(String email, EmailVerificationPurpose purpose) {
        List<EmailVerificationJpaEntity> pending = repository.findByEmailAndPurposeAndStatus(
                email, purpose, EmailVerificationStatus.PENDING);
        LocalDateTime now = LocalDateTime.now();
        for (EmailVerificationJpaEntity entity : pending) {
            entity.setStatus(EmailVerificationStatus.EXPIRED);
            entity.setExpiresAt(now);
        }
        if (!pending.isEmpty()) {
            repository.saveAll(pending);
        }
    }

    private EmailVerificationJpaEntity requireEntity(String token) {
        if (token == null || token.isBlank()) {
            throw new EmailVerificationException("유효하지 않은 인증 링크입니다.");
        }
        String challengeId;
        try {
            challengeId = tokenCodec.decrypt(token.trim());
        } catch (IllegalArgumentException e) {
            throw new EmailVerificationException("유효하지 않은 인증 링크입니다.");
        }
        return repository.findByChallengeId(challengeId)
                .orElseThrow(() -> new EmailVerificationException("유효하지 않은 인증 링크입니다."));
    }

    private boolean isExpired(EmailVerificationJpaEntity entity) {
        return entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(LocalDateTime.now());
    }

    private EmailVerificationConfirmResponse toConfirmResponse(EmailVerificationJpaEntity entity, boolean verified) {
        String username = null;
        String email = null;
        if (entity.getPurpose() == EmailVerificationPurpose.FIND_EMAIL
                || entity.getPurpose() == EmailVerificationPurpose.FIND_PASSWORD) {
            email = entity.getEmail();
        }
        if (entity.getPurpose() == EmailVerificationPurpose.FIND_EMAIL) {
            username = userPersistencePort.findByEmail(entity.getEmail())
                    .map(User::getUsername)
                    .orElse(null);
        }
        String redirectTo = switch (entity.getPurpose()) {
            case SIGNUP -> "/signup";
            case FIND_EMAIL -> "/login";
            case FIND_PASSWORD -> "/login";
        };
        return EmailVerificationConfirmResponse.builder()
                .verified(verified)
                .purpose(entity.getPurpose())
                .purposeLabel(entity.getPurposeLabel())
                .maskedEmail(maskEmail(entity.getEmail()))
                .email(email)
                .username(username)
                .redirectTo(redirectTo)
                .canResetPassword(entity.getPurpose() == EmailVerificationPurpose.FIND_PASSWORD)
                .build();
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new EmailVerificationException("이메일은 필수입니다.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.substring(0, 2) + "***" + domain;
    }
}
