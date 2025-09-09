package com.sleekydz86.finsight.core.user.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_password_history")
@Getter
@NoArgsConstructor
public class UserPasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserPasswordHistory(User user, String passwordHash) {
        this.user = user;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
    }
}