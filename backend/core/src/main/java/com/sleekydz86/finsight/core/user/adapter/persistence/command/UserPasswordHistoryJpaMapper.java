package com.sleekydz86.finsight.core.user.adapter.persistence.command;

import com.sleekydz86.finsight.core.user.domain.UserPasswordHistory;
import org.springframework.stereotype.Component;

@Component
public class UserPasswordHistoryJpaMapper {

    public UserPasswordHistory toDomain(UserPasswordHistoryJpaEntity entity) {
        return UserPasswordHistory.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .passwordHash(entity.getPasswordHash())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public UserPasswordHistoryJpaEntity toEntity(UserPasswordHistory domain, UserJpaEntity user) {
        return UserPasswordHistoryJpaEntity.builder()
                .id(domain.getId())
                .user(user)
                .passwordHash(domain.getPasswordHash())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}