package com.sleekydz86.finsight.core.portfolio.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "portfolio_share_reaction",
        uniqueConstraints = @UniqueConstraint(columnNames = {"share_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioShareReactionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "share_id", nullable = false)
    private Long shareId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reaction_type", nullable = false, length = 16)
    private String reactionType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PortfolioShareReactionJpaEntity(Long shareId, Long userId, String reactionType, LocalDateTime createdAt) {
        this.shareId = shareId;
        this.userId = userId;
        this.reactionType = reactionType;
        this.createdAt = createdAt;
    }

    public void change(String reactionType) {
        this.reactionType = reactionType;
    }
}
