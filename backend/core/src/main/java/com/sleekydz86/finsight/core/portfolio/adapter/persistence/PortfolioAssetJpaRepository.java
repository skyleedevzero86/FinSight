package com.sleekydz86.finsight.core.portfolio.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioAssetJpaRepository extends JpaRepository<PortfolioAssetJpaEntity, Long> {

    List<PortfolioAssetJpaEntity> findByUserIdOrderByValuationAmountDescIdAsc(Long userId);

    Optional<PortfolioAssetJpaEntity> findByIdAndUserId(Long id, Long userId);
}
