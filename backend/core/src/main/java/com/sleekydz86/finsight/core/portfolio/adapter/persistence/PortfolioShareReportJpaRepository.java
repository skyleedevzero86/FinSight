package com.sleekydz86.finsight.core.portfolio.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortfolioShareReportJpaRepository extends JpaRepository<PortfolioShareReportJpaEntity, Long> {

    Optional<PortfolioShareReportJpaEntity> findByShareIdAndUserId(Long shareId, Long userId);

    long countByShareId(Long shareId);
}
