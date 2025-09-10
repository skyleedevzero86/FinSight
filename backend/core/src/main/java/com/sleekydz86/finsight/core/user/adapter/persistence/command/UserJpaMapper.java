package com.sleekydz86.finsight.core.user.adapter.persistence.command;

import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.NotificationType;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.UserStatus;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Component
public class UserJpaMapper {

    public UserJpaEntity toEntity(User user) {
        if (user == null) {
            return null;
        }

        UserJpaEntity entity = UserJpaEntity.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .apiKey(user.getApiKey())
                .status(user.getStatus())
                .role(user.getRole())
                .lastLoginAt(user.getLastLoginAt())
                .loginFailCount(user.getLoginFailCount())
                .accountLockedAt(user.getAccountLockedAt())
                .approvedBy(user.getApprovedBy())
                .approvedAt(user.getApprovedAt())
                .passwordChangedAt(user.getPasswordChangedAt())
                .passwordChangeCount(user.getPasswordChangeCount())
                .lastPasswordChangeDate(user.getLastPasswordChangeDate())
                .watchlist(user.getWatchlist() != null ? new ArrayList<>(user.getWatchlist()) : new ArrayList<>())
                .notificationPreferences(
                        user.getNotificationPreferences() != null ? new ArrayList<>(user.getNotificationPreferences())
                                : new ArrayList<>())

                .otpSecret(user.getOtpSecret())
                .otpEnabled(user.getOtpEnabled())
                .otpVerified(user.getOtpVerified())

                .build();

        if (user.getId() != null) {
            setEntityIdUsingReflection(entity, user.getId());
        }
        if (user.getCreatedAt() != null) {
            setEntityCreatedAtUsingReflection(entity, user.getCreatedAt());
        }
        if (user.getUpdatedAt() != null) {
            setEntityUpdatedAtUsingReflection(entity, user.getUpdatedAt());
        }

        return entity;
    }

    public User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        User user = User.builder()
                .username(entity.getUsername())
                .password(entity.getPassword())
                .nickname(entity.getNickname())
                .email(entity.getEmail())
                .apiKey(entity.getApiKey())
                .status(entity.getStatus() != null ? entity.getStatus() : UserStatus.PENDING)
                .role(entity.getRole() != null ? entity.getRole() : UserRole.USER)
                .lastLoginAt(entity.getLastLoginAt())
                .loginFailCount(entity.getLoginFailCount() != null ? entity.getLoginFailCount() : 0)
                .accountLockedAt(entity.getAccountLockedAt())
                .approvedBy(entity.getApprovedBy())
                .approvedAt(entity.getApprovedAt())
                .passwordChangedAt(entity.getPasswordChangedAt())
                .passwordChangeCount(entity.getPasswordChangeCount() != null ? entity.getPasswordChangeCount() : 0)
                .lastPasswordChangeDate(entity.getLastPasswordChangeDate())
                .watchlist(entity.getWatchlist() != null ? new ArrayList<>(entity.getWatchlist()) : new ArrayList<>())
                .notificationPreferences(entity.getNotificationPreferences() != null
                        ? new ArrayList<>(entity.getNotificationPreferences())
                        : new ArrayList<>())

                .otpSecret(entity.getOtpSecret())
                .otpEnabled(entity.getOtpEnabled() != null ? entity.getOtpEnabled() : false)
                .otpVerified(entity.getOtpVerified() != null ? entity.getOtpVerified() : false)
                .build();

        if (entity.getId() != null) {
            setUserIdUsingReflection(user, entity.getId());
        }
        if (entity.getCreatedAt() != null) {
            setUserCreatedAtUsingReflection(user, entity.getCreatedAt());
        }
        if (entity.getUpdatedAt() != null) {
            setUserUpdatedAtUsingReflection(user, entity.getUpdatedAt());
        }

        return user;
    }

    private void setUserIdUsingReflection(User user, Long id) {
        try {
            Field idField = findIdField(user.getClass());
            if (idField != null) {
                idField.setAccessible(true);
                idField.set(user, id);
            }
        } catch (Exception e) {
            throw new RuntimeException("User ID 설정 실패", e);
        }
    }

    private void setUserCreatedAtUsingReflection(User user, java.time.LocalDateTime createdAt) {
        try {
            Field createdAtField = findCreatedAtField(user.getClass());
            if (createdAtField != null) {
                createdAtField.setAccessible(true);
                createdAtField.set(user, createdAt);
            }
        } catch (Exception e) {
            throw new RuntimeException("User CreatedAt 설정 실패", e);
        }
    }

    private void setUserUpdatedAtUsingReflection(User user, java.time.LocalDateTime updatedAt) {
        try {
            Field updatedAtField = findUpdatedAtField(user.getClass());
            if (updatedAtField != null) {
                updatedAtField.setAccessible(true);
                updatedAtField.set(user, updatedAt);
            }
        } catch (Exception e) {
            throw new RuntimeException("User UpdatedAt 설정 실패", e);
        }
    }

    private void setEntityIdUsingReflection(UserJpaEntity entity, Long id) {
        try {
            Field idField = findIdField(entity.getClass());
            if (idField != null) {
                idField.setAccessible(true);
                idField.set(entity, id);
            }
        } catch (Exception e) {
            throw new RuntimeException("Entity ID 설정 실패", e);
        }
    }

    private void setEntityCreatedAtUsingReflection(UserJpaEntity entity, java.time.LocalDateTime createdAt) {
        try {
            Field createdAtField = findCreatedAtField(entity.getClass());
            if (createdAtField != null) {
                createdAtField.setAccessible(true);
                createdAtField.set(entity, createdAt);
            }
        } catch (Exception e) {
            throw new RuntimeException("Entity CreatedAt 설정 실패", e);
        }
    }

    private void setEntityUpdatedAtUsingReflection(UserJpaEntity entity, java.time.LocalDateTime updatedAt) {
        try {
            Field updatedAtField = findUpdatedAtField(entity.getClass());
            if (updatedAtField != null) {
                updatedAtField.setAccessible(true);
                updatedAtField.set(entity, updatedAt);
            }
        } catch (Exception e) {
            throw new RuntimeException("Entity UpdatedAt 설정 실패", e);
        }
    }

    private Field findIdField(Class<?> clazz) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField("id");
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private Field findCreatedAtField(Class<?> clazz) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField("createdAt");
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private Field findUpdatedAtField(Class<?> clazz) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField("updatedAt");
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    public List<User> toDomainList(List<UserJpaEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        return entities.stream()
                .map(this::toDomain)
                .toList();
    }

    public List<UserJpaEntity> toEntityList(List<User> users) {
        if (users == null || users.isEmpty()) {
            return new ArrayList<>();
        }

        return users.stream()
                .map(this::toEntity)
                .toList();
    }
}