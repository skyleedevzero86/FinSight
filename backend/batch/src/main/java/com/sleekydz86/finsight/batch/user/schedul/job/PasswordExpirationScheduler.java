package com.sleekydz86.finsight.batch.user.schedul.job;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import com.sleekydz86.finsight.core.user.service.PasswordExpiryNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordExpirationScheduler {

    private final UserPersistencePort userPersistencePort;
    private final PasswordExpiryNoticeService passwordExpiryNoticeService;

    @Scheduled(cron = "0 0 9 * * ?")
    public void checkPasswordExpiration() {
        try {
            LocalDateTime recommendThreshold = LocalDateTime.now().minusDays(User.PASSWORD_RECOMMEND_DAYS);
            List<User> candidates = userPersistencePort.findUsersWithPasswordChangedBefore(recommendThreshold);

            int expiredCount = 0;
            int warningCount = 0;

            for (User user : candidates) {
                if (!user.isWebAccount()) {
                    continue;
                }
                if (user.isPasswordChangeRequired()) {
                    expiredCount++;
                    passwordExpiryNoticeService.notifyIfDue(user);
                } else if (user.isPasswordChangeRecommended()) {
                    warningCount++;
                    passwordExpiryNoticeService.notifyWarningIfDue(user);
                }
            }

            if (expiredCount > 0 || warningCount > 0) {
                log.info("비밀번호 만료 알림 완료: 만료 {}명, 경고 {}명", expiredCount, warningCount);
            }
        } catch (Exception e) {
            log.error("비밀번호 만료 확인 실패: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 10 * * MON")
    public void generatePasswordChangeReport() {
        try {
            LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
            long weeklyChanges = userPersistencePort.countPasswordChangesAfter(oneWeekAgo);

            LocalDateTime expiryThreshold = LocalDateTime.now().minusDays(User.PASSWORD_EXPIRY_DAYS);
            long expiredUsers = userPersistencePort.countUsersWithPasswordChangedBefore(expiryThreshold);

            LocalDateTime recommendThreshold = LocalDateTime.now().minusDays(User.PASSWORD_RECOMMEND_DAYS);
            long expiringSoonUsers = Math.max(0,
                    userPersistencePort.countUsersWithPasswordChangedBefore(recommendThreshold) - expiredUsers);

            log.info("=== 비밀번호 변경 주간 보고서 ===");
            log.info("주간 비밀번호 변경: {}건", weeklyChanges);
            log.info("만료된 비밀번호: {}명", expiredUsers);
            log.info("만료 예정 비밀번호: {}명", expiringSoonUsers);
        } catch (Exception e) {
            log.error("비밀번호 변경 보고서 생성 실패: {}", e.getMessage());
        }
    }
}
