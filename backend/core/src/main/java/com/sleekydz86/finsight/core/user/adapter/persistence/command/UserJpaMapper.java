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
        User user = new User(
                entity.getEmail(),
                entity.getPassword(),
                entity.getUsername());

        if (entity.getId() != null) {
            user.setId(entity.getId());
        }

        user.setRole(entity.getRole());
        user.setActive(entity.isActive());
        user.setLastLoginAt(entity.getLastLoginAt());
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());
        user.setWatchlist(new ArrayList<>(entity.getWatchlist()));
        user.setNotificationPreferences(new ArrayList<>(entity.getNotificationPreferences()));

        return user;
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