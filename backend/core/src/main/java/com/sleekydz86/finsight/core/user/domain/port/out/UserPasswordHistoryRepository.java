package com.sleekydz86.finsight.core.user.domain.port.out;

import com.sleekydz86.finsight.core.user.domain.UserPasswordHistory;
import java.time.LocalDateTime;
import java.util.List;

public interface UserPasswordHistoryRepository {

        UserPasswordHistory save(UserPasswordHistory passwordHistory);

        List<UserPasswordHistory> findByUserId(Long userId);

        List<UserPasswordHistory> findRecentByUserId(Long userId, int limit);

        List<UserPasswordHistory> findRecentPasswordHistoryWithLimit(Long userId, int limit);

        long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);

        long countPasswordChangesAfter(Long userId, LocalDateTime afterDateTime);

        long countTodayPasswordChanges(Long userId, LocalDateTime today);

        void deleteById(Long id);

        void deleteByUserId(Long userId);
}