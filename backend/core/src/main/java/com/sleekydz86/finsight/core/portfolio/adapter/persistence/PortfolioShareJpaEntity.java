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
@Table(name = "portfolio_share")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioShareJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "author_name", nullable = false, length = 50)
    private String authorName;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false, length = 16)
    private String visibility;

    @Column(name = "goal_label", nullable = false, length = 40)
    private String goalLabel;

    @Column(name = "amount_label", nullable = false, length = 40)
    private String amountLabel;

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    @Column(name = "month_rate_label", length = 20)
    private String monthRateLabel;

    @Column(name = "goal_rate_label", length = 20)
    private String goalRateLabel;

    @Column(name = "show_asset", nullable = false)
    private boolean showAsset;

    @Column(name = "show_debt", nullable = false)
    private boolean showDebt;

    @Column(name = "cheer_count", nullable = false)
    private int cheerCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PortfolioShareJpaEntity(
            Long userId,
            String authorName,
            String message,
            String visibility,
            String goalLabel,
            String amountLabel,
            int progressPercent,
            String monthRateLabel,
            String goalRateLabel,
            boolean showAsset,
            boolean showDebt,
            LocalDateTime createdAt) {
        this.userId = userId;
        this.authorName = authorName;
        this.message = message;
        this.visibility = visibility;
        this.goalLabel = goalLabel;
        this.amountLabel = amountLabel;
        this.progressPercent = progressPercent;
        this.monthRateLabel = monthRateLabel;
        this.goalRateLabel = goalRateLabel;
        this.showAsset = showAsset;
        this.showDebt = showDebt;
        this.cheerCount = 0;
        this.createdAt = createdAt;
    }
}
