package com.sleekydz86.finsight.core.user.adapter.persistence.command;

import com.sleekydz86.finsight.core.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_password_history")
@Getter
@Setter
@NoArgsConstructor
public class UserPasswordHistoryJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserPasswordHistoryJpaEntity(Long id, UserJpaEntity user, String passwordHash, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}