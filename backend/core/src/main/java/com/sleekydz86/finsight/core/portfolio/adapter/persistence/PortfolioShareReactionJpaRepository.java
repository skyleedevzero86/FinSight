package com.sleekydz86.finsight.core.portfolio.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortfolioShareReactionJpaRepository extends JpaRepository<PortfolioShareReactionJpaEntity, Long> {

    Optional<PortfolioShareReactionJpaEntity> findByShareIdAndUserId(Long shareId, Long userId);

    long countByShareIdAndReactionType(Long shareId, String reactionType);
}
