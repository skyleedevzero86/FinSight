package com.sleekydz86.finsight.core.user.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class UserPasswordHistory {

    private Long id;
    private Long userId;
    private String passwordHash;
    private LocalDateTime createdAt;

    @Builder
    public UserPasswordHistory(Long id, Long userId, String passwordHash, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}