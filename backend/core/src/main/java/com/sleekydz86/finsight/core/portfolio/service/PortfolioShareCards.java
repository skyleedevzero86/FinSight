package com.sleekydz86.finsight.core.portfolio.service;

import com.sleekydz86.finsight.core.portfolio.adapter.persistence.PortfolioShareJpaEntity;
import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareFeed.PortfolioShareCard;

import java.time.format.DateTimeFormatter;

public final class PortfolioShareCards {

    private static final DateTimeFormatter SHARE_DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private PortfolioShareCards() {
    }

    public static PortfolioShareCard from(PortfolioShareJpaEntity share, long likes, long dislikes, long reports) {
        String stored = share.getModerationStatus();
        String status = stored == null || stored.isBlank() ? "OPEN" : stored;
        String sharedAt = share.getCreatedAt() == null ? "" : SHARE_DATE.format(share.getCreatedAt());
        return new PortfolioShareCard(
                share.getId() == null ? 0L : share.getId(),
                PortfolioShareText.maskName(share.getAuthorName()),
                share.getMessage(),
                share.getGoalLabel(),
                share.getAmountLabel(),
                share.getProgressPercent(),
                share.getMonthRateLabel(),
                share.getGoalRateLabel(),
                share.isShowAsset(),
                share.isShowDebt(),
                share.getCheerCount(),
                sharedAt,
                PortfolioShareSignals.display(status, dislikes, reports),
                likes,
                dislikes,
                reports);
    }
}
