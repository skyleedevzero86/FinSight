package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.notification.adapter.persistence.WelcomeEmailJobJpaRepository;
import com.sleekydz86.finsight.core.notification.domain.WelcomeEmailJob;
import com.sleekydz86.finsight.core.notification.domain.WelcomeEmailJob.Status;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WelcomeEmailDispatchService {

    private static final Logger log = LoggerFactory.getLogger(WelcomeEmailDispatchService.class);

    private final WelcomeEmailJobJpaRepository welcomeEmailJobJpaRepository;
    private final EmailNotificationService emailNotificationService;
    private final UserPersistencePort userPersistencePort;

    @Value("${app.mail.welcome.window-hours:2}")
    private long windowHours;

    @Value("${app.mail.welcome.initial-delay-seconds:30}")
    private long initialDelaySeconds;

    @Value("${app.mail.welcome.max-attempts:3}")
    private int maxAttempts;

    public WelcomeEmailDispatchService(
            WelcomeEmailJobJpaRepository welcomeEmailJobJpaRepository,
            EmailNotificationService emailNotificationService,
            UserPersistencePort userPersistencePort) {
        this.welcomeEmailJobJpaRepository = welcomeEmailJobJpaRepository;
        this.emailNotificationService = emailNotificationService;
        this.userPersistencePort = userPersistencePort;
    }

    @Transactional
    public void enqueue(Long userId, LocalDateTime registeredAt) {
        if (userId == null) {
            return;
        }
        if (welcomeEmailJobJpaRepository.existsByUserId(userId)) {
            log.debug("환영 메일 작업이 이미 존재합니다. userId={}", userId);
            return;
        }

        LocalDateTime registered = registeredAt != null ? registeredAt : LocalDateTime.now();
        LocalDateTime deadline = registered.plusHours(windowHours);
        LocalDateTime scheduled = LocalDateTime.now().plusSeconds(Math.max(0, initialDelaySeconds));
        if (scheduled.isAfter(deadline)) {
            scheduled = registered;
        }

        WelcomeEmailJob job = new WelcomeEmailJob(userId, registered, deadline, scheduled);
        welcomeEmailJobJpaRepository.save(job);
        log.info("회원가입 축하 메일 예약: userId={}, scheduledAt={}, deadlineAt={}",
                userId, scheduled, deadline);
    }

    @Transactional
    public void processDueJobs() {
        LocalDateTime now = LocalDateTime.now();
        expireOverdue(now);

        List<WelcomeEmailJob> due = welcomeEmailJobJpaRepository.findDuePending(Status.PENDING, now);
        for (WelcomeEmailJob job : due) {
            dispatch(job, now);
        }
    }

    private void expireOverdue(LocalDateTime now) {
        List<WelcomeEmailJob> expired = welcomeEmailJobJpaRepository.findExpiredPending(Status.PENDING, now);
        for (WelcomeEmailJob job : expired) {
            job.markExpired();
            welcomeEmailJobJpaRepository.save(job);
            log.warn("회원가입 축하 메일 기한 만료: userId={}, deadlineAt={}",
                    job.getUserId(), job.getDeadlineAt());
        }
    }

    private void dispatch(WelcomeEmailJob job, LocalDateTime now) {
        if (job.getDeadlineAt().isBefore(now)) {
            job.markExpired();
            welcomeEmailJobJpaRepository.save(job);
            return;
        }

        User user = userPersistencePort.findById(job.getUserId()).orElse(null);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            job.markFailed("사용자를 찾을 수 없거나 이메일이 없습니다.");
            welcomeEmailJobJpaRepository.save(job);
            return;
        }

        try {
            emailNotificationService.sendWelcomeEmailSync(user);
            job.markSent();
            welcomeEmailJobJpaRepository.save(job);
            log.info("회원가입 축하 메일 발송 완료: userId={}", job.getUserId());
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (job.getAttemptCount() + 1 >= maxAttempts || now.plusMinutes(5).isAfter(job.getDeadlineAt())) {
                job.markFailed(message);
                log.error("회원가입 축하 메일 최종 실패: userId={}, error={}", job.getUserId(), message);
            } else {
                LocalDateTime retryAt = now.plusMinutes(5);
                if (retryAt.isAfter(job.getDeadlineAt())) {
                    retryAt = job.getDeadlineAt();
                }
                job.retryLater(retryAt, message);
                log.warn("회원가입 축하 메일 재시도 예약: userId={}, retryAt={}, error={}",
                        job.getUserId(), retryAt, message);
            }
            welcomeEmailJobJpaRepository.save(job);
        }
    }
}
