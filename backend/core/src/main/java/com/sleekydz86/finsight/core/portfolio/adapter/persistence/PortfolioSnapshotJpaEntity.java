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
@Table(name = "portfolio_snapshot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioSnapshotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "snapshot_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "total_assets", nullable = false)
    private long totalAssets;

    @Column(name = "total_liabilities", nullable = false)
    private long totalLiabilities;

    @Column(name = "net_worth", nullable = false)
    private long netWorth;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(length = 500)
    private String memo;

    @Column(name = "record_status", nullable = false, length = 16)
    private String recordStatus;

    public PortfolioSnapshotJpaEntity(
            Long userId,
            String yearMonth,
            long totalAssets,
            long totalLiabilities,
            long netWorth,
            LocalDateTime recordedAt,
            String memo) {
        this.userId = userId;
        this.yearMonth = yearMonth;
        this.totalAssets = totalAssets;
        this.totalLiabilities = totalLiabilities;
        this.netWorth = netWorth;
        this.recordedAt = recordedAt;
        this.memo = memo;
        this.recordStatus = "CONFIRMED";
    }

    public void replace(long totalAssets, long totalLiabilities, long netWorth, LocalDateTime recordedAt, String memo) {
        this.totalAssets = totalAssets;
        this.totalLiabilities = totalLiabilities;
        this.netWorth = netWorth;
        this.recordedAt = recordedAt;
        this.memo = memo;
        this.recordStatus = "CONFIRMED";
    }
}
