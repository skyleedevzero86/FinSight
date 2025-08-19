package com.sleekydz86.finsight.core.user.domain.port.in;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.NotificationType;

import java.util.List;
import java.util.Optional;

public interface UserQueryUseCase {
    Optional<User> findById(Long userId);

    Optional<User> findByEmail(String email);

    List<TargetCategory> getUserWatchlist(Long userId);

    List<NotificationType> getUserNotificationPreferences(Long userId);
}