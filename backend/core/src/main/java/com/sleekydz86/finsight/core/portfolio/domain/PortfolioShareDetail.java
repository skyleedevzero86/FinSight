package com.sleekydz86.finsight.core.portfolio.domain;

import com.sleekydz86.finsight.core.portfolio.domain.PortfolioShareFeed.PortfolioShareCard;

public record PortfolioShareDetail(
        PortfolioShareCard card,
        String myReaction,
        boolean reportedByMe,
        PortfolioShareView view
) {
}
