package com.sleekydz86.finsight.core.portfolio.domain;

public record PortfolioShareCommentItem(
        long id,
        String authorName,
        String content,
        String createdAt
) {
}
