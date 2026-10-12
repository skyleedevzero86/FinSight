package com.sleekydz86.finsight.core.portfolio.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_share_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioShareCommentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "share_id", nullable = false)
    private Long shareId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "author_name", nullable = false, length = 50)
    private String authorName;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PortfolioShareCommentJpaEntity(
            Long shareId,
            Long userId,
            String authorName,
            String content,
            LocalDateTime createdAt) {
        this.shareId = shareId;
        this.userId = userId;
        this.authorName = authorName;
        this.content = content;
        this.createdAt = createdAt;
    }
}
