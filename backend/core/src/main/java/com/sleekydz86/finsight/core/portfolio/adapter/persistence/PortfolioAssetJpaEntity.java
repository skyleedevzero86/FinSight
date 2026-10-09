package com.sleekydz86.finsight.core.portfolio.adapter.persistence;

import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetCategory;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioAssetKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_asset")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioAssetJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "asset_name", nullable = false, length = 80)
    private String assetName;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_kind", nullable = false, length = 16)
    private PortfolioAssetKind assetKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_category", nullable = false, length = 32)
    private PortfolioAssetCategory assetCategory;

    @Column(name = "valuation_amount", nullable = false)
    private long valuationAmount;

    @Column(name = "profit_rate", precision = 8, scale = 2)
    private BigDecimal profitRate;

    @Column(length = 80)
    private String custodian;

    @Column(name = "acquisition_amount")
    private Long acquisitionAmount;

    @Column(precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price")
    private Long unitPrice;

    @Column(length = 500)
    private String memo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PortfolioAssetJpaEntity(
            Long userId,
            String assetName,
            PortfolioAssetKind assetKind,
            PortfolioAssetCategory assetCategory,
            long valuationAmount,
            BigDecimal profitRate,
            String custodian,
            Long acquisitionAmount,
            BigDecimal quantity,
            Long unitPrice,
            String memo) {
        this.userId = userId;
        this.assetName = assetName;
        this.assetKind = assetKind;
        this.assetCategory = assetCategory;
        this.valuationAmount = valuationAmount;
        this.profitRate = profitRate;
        this.custodian = custodian;
        this.acquisitionAmount = acquisitionAmount;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.memo = memo;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
