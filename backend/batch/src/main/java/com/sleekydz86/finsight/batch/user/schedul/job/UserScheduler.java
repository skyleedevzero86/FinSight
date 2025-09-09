package com.sleekydz86.finsight.batch.user.schedul.job;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserStatus;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserScheduler {

    private final UserPersistencePort userPersistencePort;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void unlockExpiredAccounts() {
        try {
            LocalDateTime unlockTime = LocalDateTime.now().minusHours(24);
            List<User> lockedUsers = userPersistencePort.findLockedUsersBeforeUnlockTime(unlockTime);

            int unlockedCount = 0;
            for (User user : lockedUsers) {
                if (user.getStatus() == UserStatus.SUSPENDED && user.getAccountLockedAt() != null) {
                    user.unlock();
                    userPersistencePort.save(user);
                    unlockedCount++;
                }
            }

            if (unlockedCount > 0) {
                log.info("자동 계정 잠금 해제 완료: {}명", unlockedCount);
            }
        } catch (Exception e) {
            log.error("자동 계정 잠금 해제 실패: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 9 * * MON")
    @Transactional(readOnly = true)
    public void notifyPendingUsers() {
        try {
            LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
            List<User> pendingUsers = userPersistencePort.findPendingUsersAfter(UserStatus.PENDING, oneWeekAgo);

            if (!pendingUsers.isEmpty()) {
                log.info("승인 대기 중인 사용자 알림: {}명", pendingUsers.size());
                for (User user : pendingUsers) {
                    log.info("승인 대기 사용자: {} ({})", user.getUsername(), user.getEmail());
                }
            }
        } catch (Exception e) {
            log.error("승인 대기 사용자 알림 실패: {}", e.getMessage());
        }
    }
}