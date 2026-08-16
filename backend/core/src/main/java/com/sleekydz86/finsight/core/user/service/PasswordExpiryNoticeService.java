package com.sleekydz86.finsight.core.user.service;

import com.sleekydz86.finsight.core.notification.service.EmailNotificationService;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordExpiryNoticeService {

    private static final Logger log = LoggerFactory.getLogger(PasswordExpiryNoticeService.class);

    private final EmailNotificationService emailNotificationService;
    private final UserPersistencePort userPersistencePort;

    public PasswordExpiryNoticeService(
            EmailNotificationService emailNotificationService,
            UserPersistencePort userPersistencePort) {
        this.emailNotificationService = emailNotificationService;
        this.userPersistencePort = userPersistencePort;
    }

    @Transactional
    public void notifyIfDue(User user) {
        if (user == null || !user.shouldSendPasswordExpiryMail()) {
            return;
        }
        try {
            emailNotificationService.sendPasswordChangeReminder(user, false);
            user.markPasswordExpiryNotified();
            userPersistencePort.save(user);
        } catch (Exception e) {
            log.warn("비밀번호 만료 안내 메일 발송 실패: userId={}, error={}", user.getId(), e.getMessage());
        }
    }

    @Transactional
    public void notifyWarningIfDue(User user) {
        if (user == null || !user.isWebAccount() || user.isPasswordChangeRequired()) {
            return;
        }
        if (!user.isPasswordChangeRecommended()) {
            return;
        }
        if (user.getPasswordExpiryNotifiedAt() != null
                && !user.getPasswordExpiryNotifiedAt().isBefore(java.time.LocalDateTime.now().minusHours(24))) {
            return;
        }
        try {
            emailNotificationService.sendPasswordChangeReminder(user, true);
            user.markPasswordExpiryNotified();
            userPersistencePort.save(user);
        } catch (Exception e) {
            log.warn("비밀번호 만료 예정 안내 메일 발송 실패: userId={}, error={}", user.getId(), e.getMessage());
        }
    }
}
