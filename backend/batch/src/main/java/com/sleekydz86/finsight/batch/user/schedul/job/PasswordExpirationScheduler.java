package com.sleekydz86.finsight.batch.user.schedul.job;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserStatus;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
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

    @Scheduled(cron = "0 0 9 * * ?")
    public void checkPasswordExpiration() {
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            List<User> expiredUsers = userPersistencePort.findUsersWithPasswordChangedBefore(thirtyDaysAgo);

            int expiredCount = 0;
            int warningCount = 0;

            for (User user : expiredUsers) {
                if (user.getPasswordChangedAt() != null) {
                    long daysSinceChange = java.time.temporal.ChronoUnit.DAYS.between(
                            user.getPasswordChangedAt().toLocalDate(),
                            java.time.LocalDate.now()
                    );

                    if (daysSinceChange >= 30) {
                        expiredCount++;
                        sendPasswordExpirationAlert(user);
                    } else if (daysSinceChange >= 25) {
                        warningCount++;
                        sendPasswordExpirationWarning(user);
                    }
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

            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            long expiredUsers = userPersistencePort.countUsersWithPasswordChangedBefore(thirtyDaysAgo);

            LocalDateTime twentyFiveDaysAgo = LocalDateTime.now().minusDays(25);
            long expiringSoonUsers = userPersistencePort.countUsersWithPasswordChangedBefore(twentyFiveDaysAgo) - expiredUsers;

            sendWeeklyPasswordReport(weeklyChanges, expiredUsers, expiringSoonUsers);

            log.info("비밀번호 변경 주간 보고서 생성 완료: 주간 변경 {}, 만료 {}, 만료 예정 {}",
                    weeklyChanges, expiredUsers, expiringSoonUsers);
        } catch (Exception e) {
            log.error("비밀번호 변경 보고서 생성 실패: {}", e.getMessage());
        }
    }

    private void sendPasswordExpirationWarning(User user) {
        log.info("비밀번호 만료 경고 알림: {} ({})", user.getUsername(), user.getEmail());
    }

    private void sendPasswordExpirationAlert(User user) {
        log.info("비밀번호 만료 알림: {} ({})", user.getUsername(), user.getEmail());
    }

    private void sendWeeklyPasswordReport(long weeklyChanges, long expiredUsers, long expiringSoonUsers) {
        log.info("=== 비밀번호 변경 주간 보고서 ===");
        log.info("주간 비밀번호 변경: {}건", weeklyChanges);
        log.info("만료된 비밀번호: {}명", expiredUsers);
        log.info("만료 예정 비밀번호: {}명", expiringSoonUsers);
    }
}