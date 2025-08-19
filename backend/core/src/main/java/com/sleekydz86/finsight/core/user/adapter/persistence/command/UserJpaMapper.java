package com.sleekydz86.finsight.core.user.adapter.persistence.command;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.NotificationType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class UserJpaMapper {

    public User toDomain(UserJpaEntity entity) {
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getUsername(),
                entity.getRole(),
                entity.isActive(),
                entity.getLastLoginAt(),
                new ArrayList<>(entity.getWatchlist()),
                new ArrayList<>(entity.getNotificationPreferences()));
    }

    public UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getUsername(),
                user.getRole(),
                user.isActive(),
                user.getLastLoginAt(),
                new ArrayList<>(user.getWatchlist()),
                new ArrayList<>(user.getNotificationPreferences()));
    }
}