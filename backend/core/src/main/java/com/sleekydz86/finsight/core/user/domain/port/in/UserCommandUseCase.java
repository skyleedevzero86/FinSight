package com.sleekydz86.finsight.core.user.domain.port.in;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserRegistrationRequest;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserUpdateRequest;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.WatchlistUpdateRequest;
import com.sleekydz86.finsight.core.user.domain.NotificationType;

import java.util.List;

public interface UserCommandUseCase {
    User registerUser(UserRegistrationRequest request);

    User updateUser(Long userId, UserUpdateRequest request);

    void updateWatchlist(Long userId, WatchlistUpdateRequest request);

    void updateNotificationPreferences(Long userId, List<NotificationType> preferences);
}